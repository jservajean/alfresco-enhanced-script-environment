/*
 * Copyright 2013 PRODYNA AG
 *
 * Licensed under the Eclipse Public License (EPL), Version 1.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.opensource.org/licenses/eclipse-1.0.php or
 * http://www.nabucco.org/License.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org.nabucco.alfresco.enhScriptEnv.repo.script;

import java.util.Map;

import org.alfresco.repo.jscript.ValueConverter;
import org.alfresco.scripts.ScriptException;
import org.alfresco.service.cmr.repository.ScriptLocation;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdFunctionCall;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Import function to allow inclusion of other scripts in arbitrary contexts of a script in execution. This function utilizes the central
 * registry of script locators supplied by the Rhino script processor as well as its compilation / caching framework for constituent
 * scripts.
 * 
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ImportScriptFunction implements IdFunctionCall
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ImportScriptFunction.class);

    public static final int IMPORT_FUNC_ID = 0;
    public static final Object IMPORT_FUNC_TAG = new Object();
    public static final String IMPORT_FUNC_NAME = "importScript";
    public static final int ARITY = 2;

    protected final EnhancedScriptProcessor scriptProcessor;
    protected final Map<String, ScriptLocator> scriptLocators;

    /** Base Value Converter */
    protected final ValueConverter valueConverter = new ValueConverter();

    /**
     * @param scriptLocators
     */
    public ImportScriptFunction(final EnhancedScriptProcessor scriptProcessor, final Map<String, ScriptLocator> scriptLocators)
    {
        super();
        this.scriptProcessor = scriptProcessor;
        this.scriptLocators = scriptLocators;
    }

    @Override
    public Object execIdCall(final IdFunctionObject f, final Context cx, final Scriptable scope, final Scriptable thisObj,
            final Object[] args)
    {
        boolean result = false;
        if (f.hasTag(IMPORT_FUNC_TAG))
        {
            if (f.methodId() == IMPORT_FUNC_ID)
            {
                final ScriptLocation referenceLocation = this.scriptProcessor.getContextScriptLocation();

                final String locatorType = ScriptRuntime.toString(args, 0);
                final String locationValue = ScriptRuntime.toString(args, 1);

                // optional parameters
                // defaults to false
                final boolean failOnMissingScript = ScriptRuntime.toBoolean(args, 2);
                // defaults to null
                final Scriptable resolutionParams = ScriptRuntime.toObjectOrNull(cx, args.length > 3 ? args[3] : null);
                final Object resolotionParamsJavaObj = this.valueConverter.convertValueForJava(resolutionParams);
                final Scriptable executionScopeParam = ScriptRuntime.toObjectOrNull(cx, args.length > 4 ? args[4] : null);

                final ScriptLocator scriptLocator = this.scriptLocators.get(locatorType);
                if (scriptLocator != null)
                {
                    final ScriptLocation location;
                    if (resolutionParams == null)
                    {
                        location = scriptLocator.resolveLocation(referenceLocation, locationValue);
                    }
                    else if (resolotionParamsJavaObj instanceof Map<?, ?>)
                    {
                        // we know the generic parameters from the way this.valueConverter works
                        @SuppressWarnings("unchecked")
                        final Map<String, Object> resolotionParamsJavaMap = (Map<String, Object>) resolotionParamsJavaObj;
                        location = scriptLocator.resolveLocation(referenceLocation, locationValue, resolotionParamsJavaMap);
                    }
                    else
                    {
                        LOGGER.warn(
                                "Invalid parameter object for resolution of script location [{}] via locator [{}] - should have been a string-keyed map: [{}]",
                                new Object[] { locationValue, locatorType, resolotionParamsJavaObj });
                        location = null;
                    }

                    if (location == null)
                    {
                        LOGGER.info("Unable to resolve script location [{}] via locator [{}]", locationValue, locatorType);

                        if (failOnMissingScript)
                        {
                            // TODO: proper msgId
                            throw new ScriptException("Unable to resolve script location [{0}] via locator [{1}]", new Object[] {
                                    locationValue, locatorType });
                        }
                    }
                    else
                    {
                        final Scriptable executionScope;
                        if (executionScopeParam != null)
                        {
                            if (location.isSecure())
                            {
                                executionScope = new ImporterTopLevel(cx, false);
                            }
                            else
                            {
                                executionScope = cx.initStandardObjects();
                                // remove security issue related objects - this ensures the script may not access
                                // insecure java.* libraries or import any other classes for direct access - only
                                // the configured root host objects will be available to the script writer
                                executionScope.delete("Packages");
                                executionScope.delete("getClass");
                                executionScope.delete("java");
                            }

                            executionScope.setParentScope(null);
                            executionScope.setPrototype(executionScopeParam);
                        }
                        else
                        {
                            // TODO: What about insecure script called from secure one?
                            // TODO: How to deny access to Packages/getClass/java without denying access to other scope elements?
                            executionScope = scope;
                        }

                        this.scriptProcessor.executeInScope(location, executionScope);
                        result = true;
                    }
                }
                else
                {
                    LOGGER.warn("Unknown script locator [{}]", locatorType);

                    if (failOnMissingScript)
                    {
                        // TODO: proper msgId
                        throw new ScriptException("Unknown script locator [{0}]", new Object[] { locatorType });
                    }
                }
            }
            else
            {
                throw new IllegalArgumentException(String.valueOf(f.methodId()));
            }
        }
        else
        {
            throw f.unknown();
        }

        return Boolean.valueOf(result);
    }
}