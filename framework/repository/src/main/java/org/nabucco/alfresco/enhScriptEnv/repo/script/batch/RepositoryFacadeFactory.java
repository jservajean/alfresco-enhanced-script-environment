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
package org.nabucco.alfresco.enhScriptEnv.repo.script.batch;

import org.alfresco.repo.jscript.BaseScopableProcessorExtension;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.nabucco.alfresco.enhScriptEnv.common.script.batch.DefaultFacadeFactory;
import org.nabucco.alfresco.enhScriptEnv.common.script.batch.ObjectFacadingDelegator;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 *
 *
 */
public class RepositoryFacadeFactory extends DefaultFacadeFactory
{

    /**
     * {@inheritDoc}
     */
    @Override
    public Scriptable toFacadedObject(final Scriptable obj, final Scriptable referenceScope, final String accessName)
    {
        final Scriptable facadedObject = super.toFacadedObject(obj, referenceScope, accessName);

        if (facadedObject instanceof ObjectFacadingDelegator)
        {
            final Scriptable delegee = ((ObjectFacadingDelegator) facadedObject).getDelegee();
            if (delegee instanceof NativeJavaObject)
            {
                final NativeJavaObject nativeJavaObj = (NativeJavaObject) delegee;
                final Object javaObj = nativeJavaObj.unwrap();

                // we know scopeable processor extensions keep their scope in a ThreadLocal which we may have to
                // lazily-initialize for a worker thread
                if (javaObj instanceof BaseScopableProcessorExtension)
                {
                    final BaseScopableProcessorExtension scopeable = (BaseScopableProcessorExtension) javaObj;
                    if (scopeable.getScope() == null)
                    {
                        scopeable.setScope(referenceScope);
                    }
                }
            }
        }
        return facadedObject;
    }

}
