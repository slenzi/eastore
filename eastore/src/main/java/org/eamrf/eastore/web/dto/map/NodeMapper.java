package org.eamrf.eastore.web.dto.map;

import java.util.ArrayList;
import java.util.List;

import org.eamrf.core.util.CollectionUtil;
import org.eamrf.eastore.web.dto.model.NodeDto;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.Node;

/**
 * 
 * @author slenzi
 *
 */
public class NodeMapper {

	public NodeMapper() {

	}
	
	/**
	 * Map node entity to node dto.
	 * 
	 * @param node
	 * @return
	 */
	public NodeDto map(Node node) {
		
		if(node == null) {
			return null;
		}
		
		NodeDto dto = new NodeDto();
		
		dto.setNodeId(node.getNodeId());
		dto.setParentNodeId(node.getParentNodeId());
		dto.setChildNodeId(node.getChildNodeId());
		dto.setNodeName(node.getNodeName());
		dto.setDateCreated(node.getDateCreated());
		dto.setDateUpdated(node.getDateUpdated());
		
		return dto;
		
	}
	
	/**
	 * Map list of node entities to list of node dtos
	 * 
	 * @param nodes
	 * @return
	 */
	public List<NodeDto> map(List<Node> nodes){
		
		if(CollectionUtil.isEmpty(nodes)) {
			return null;
		}
		List<NodeDto> dtoList = new ArrayList<NodeDto>();
		for(Node n : nodes) {
			dtoList.add(map(n));
		}
		return dtoList;
		
	}

}
