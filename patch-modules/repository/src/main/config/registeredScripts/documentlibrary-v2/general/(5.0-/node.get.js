importScript("registry", "evaluator.lib@documentlibrary-v2", true, importVersionCondition);
importScript("registry", "parse-args.lib@documentlibrary-v2", true, importVersionCondition);

/**
 * Main entry point: Return single document or folder given it's nodeRef
 *
 * @method getDoclist
 */
function getDoclist()
{
   // Use helper function to get the arguments
   var parsedArgs = ParseArgs.getParsedArgs();
   if (parsedArgs === null)
   {
      return;
   }

   parsedArgs.pathNode = ParseArgs.resolveNode(parsedArgs.nodeRef);
   parsedArgs.location = Common.getLocation(parsedArgs.pathNode, parsedArgs.libraryRoot);

   var favourites = Common.getFavourites(),
      node = parsedArgs.pathNode;

   var thumbnail = null,
       item = Evaluator.run(node);

   item.isFavourite = (favourites[node.nodeRef] === true);
   item.likes = Common.getLikes(node);
   item.location =
   {
      site: parsedArgs.location.site,
      siteTitle: parsedArgs.location.siteTitle,
      container: parsedArgs.location.container,
      containerType: parsedArgs.location.containerType,
      path: parsedArgs.location.path,
      file: node.name
   };

   item.parent = {};
   if (node.parent != null && node.parent.isContainer && node.parent.hasPermission("Read"))
   {
      item.parent = Evaluator.run(node.parent, true);
   }

   // Special case for container and libraryRoot nodes
   if ((parsedArgs.location.containerNode && String(parsedArgs.location.containerNode.nodeRef) == String(node.nodeRef)) ||
      (parsedArgs.libraryRoot && String(parsedArgs.libraryRoot.nodeRef) == String(node.nodeRef)))
   {
      item.location.file = "";
   }
      
   var returnObject = {
      container: parsedArgs.rootNode,
      onlineEditing: utils.moduleInstalled("org.alfresco.module.vti"),
      item: item,
      customJSON: slingshotDocLib.getJSON()
   };
   
   // Additionally include the content if requested by request parameter...
   if (args["includeContent"] == "true")
   {
      returnObject.content = item.node.content
   }
   if (args["includeThumbnails"] == "true")
   {
      returnObject.thumbnailDefinitions = item.node.thumbnailDefinitions
   }
   return (returnObject);
}

/**
 * Document List Component: doclist
 */
model.doclist = getDoclist();