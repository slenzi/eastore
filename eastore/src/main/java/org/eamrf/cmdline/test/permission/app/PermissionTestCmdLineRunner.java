/**
 * 
 */
package org.eamrf.cmdline.test.permission.app;

import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.eamrf.core.logging.stereotype.InjectLogger;
import org.eamrf.eastore.core.service.FileSystemService;
import org.eamrf.eastore.core.service.NodeTreeService;
import org.eamrf.eastore.core.service.PathResourceTreeService;
import org.eamrf.eastore.core.service.PathResourceTreeUtil;
import org.eamrf.eastore.core.tree.ToString;
import org.eamrf.eastore.core.tree.Tree;
import org.eamrf.eastore.core.tree.TreeNode;
import org.eamrf.eastore.core.tree.TreeNodeVisitException;
import org.eamrf.eastore.core.tree.TreeNodeVisitor;
import org.eamrf.eastore.core.tree.Trees.PrintOption;
import org.eamrf.eastore.core.tree.Trees.WalkOption;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.Node;
import org.eamrf.repository.jdbc.oracle.ecoguser.eastore.model.impl.PathResource;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Command line runner for testing eastore/gatekeeper permissions
 * 
 * @author slenzi
 */
@Order(1)
@Component
public class PermissionTestCmdLineRunner implements CommandLineRunner {

    @InjectLogger
    private Logger logger;
    
    //@Autowired
    //private NodeTreeService nodeTreeService;
    
    //@Autowired
    //private PathResourceTreeUtil pathResourceTreUtil;
    
    //@Autowired
    //private PathResourceTreeService pathResourceTreeService;
	
	public PermissionTestCmdLineRunner() {

	}

	/* (non-Javadoc)
	 * @see org.springframework.boot.CommandLineRunner#run(java.lang.String[])
	 */
	@Override
	public void run(String... arg0) throws Exception {
		
		logger.info(PermissionTestCmdLineRunner.class.getName() + " running...");
		
		System.out.println("Here");
		
		//PathResource resource = fileSystemService.getPathResource(100L);
		//Tree<PathResource> tree = pathResourceTreeService.buildPathResourceTree(100L);
		//pathResourceTreUtil.logTree(tree);
		
		TreeNode<MyNode> t0 = new TreeNode<MyNode>(new MyNode(0, Arrays.asList(1,2,3), Arrays.asList(1,2,3)) );
		TreeNode<MyNode> t1 = new TreeNode<MyNode>(new MyNode(1, Arrays.asList(), Arrays.asList()) );
		TreeNode<MyNode> t2 = new TreeNode<MyNode>(new MyNode(2, Arrays.asList(), Arrays.asList()) );
		TreeNode<MyNode> t3 = new TreeNode<MyNode>(new MyNode(3, Arrays.asList(4,5,6), Arrays.asList(4,5,6)) );
		TreeNode<MyNode> t4 = new TreeNode<MyNode>(new MyNode(4, Arrays.asList(), Arrays.asList(4)) );
		TreeNode<MyNode> t5 = new TreeNode<MyNode>(new MyNode(5, Arrays.asList(), Arrays.asList()) );
		TreeNode<MyNode> t6 = new TreeNode<MyNode>(new MyNode(6, Arrays.asList(7,8,9), Arrays.asList(7,8,9)) );
		TreeNode<MyNode> t7 = new TreeNode<MyNode>(new MyNode(7, Arrays.asList(4), Arrays.asList()) );
		
		t0.addChildNode(t1);
			t1.addChildNode(t3);
				t3.addChildNode(t5);
			t1.addChildNode(t4);
				t4.addChildNode(t6);
		t0.addChildNode(t2);
			t2.addChildNode(t7);
		
		Tree<MyNode> myTree = new Tree<MyNode>(t0);
		
		logger.info("Before setting permission bits:\n" + myTree.printTree((node) -> { return node.toString(); }) );
		
		setPermissionBits(4, myTree, Access.ALLOW);
		
		logger.info("After setting permission bits:\n" + myTree.printTree((node) -> { return node.toString(); }) );
		
		logger.info("Done with tests.");
		
		System.exit(1);

	}
	
	private enum Access {
		
		// nodes with no read/write groups are by default open to everyone.
		ALLOW,
		
		// nodes with no read/write groups are by default closed to everyone.
		// access will be inherited from their parent. If parent has no read/write
		// group then access is denied.
		DENY;
		
	}
	
	/**
	 * Walk the tree in pre-order traversal and set the read & write bits for the user using the read & write
	 * access groups for each node.
	 * 
	 * Nodes that don't have read & write permissions inherit the permissions from their parent
	 * 
	 * @param userId - The users ID
	 * @param tree - The tree to walk
	 * @throws TreeNodeVisitException
	 */
	public void setPermissionBits(Integer userId, Tree<MyNode> tree, Access access) throws TreeNodeVisitException {
		
		setPermissionBits(
				userId, 
				access, 
				tree.getRootNode(), 
				tree.getRootNode().getData().getReadMembers(), 
				tree.getRootNode().getData().getWriteMembers());
		
	}

	private void setPermissionBits(Integer userId, Access access, TreeNode<MyNode> node, List<Integer> readGroup, List<Integer> writeGroup) {
		
		MyNode mynode = node.getData();
		
		logger.info("Setting read permssion bit for node " + mynode.toString());
		
		// use read & write groups from the node to determine read & write permissions for the user.
		// if the node doesn't have a read or write group then use the read & write groups from
		// the last parent that had one. nodes that don't have read & write permissions inherit the
		// permissions from their parent.
		
		// set read bit
		if(mynode.getReadMembers() != null && mynode.getReadMembers().size() > 0) {
			readGroup = mynode.getReadMembers();
			if(readGroup.contains(userId)) {
				mynode.setCanRead(true);
			}			
		}else {
			if(access == Access.ALLOW) {
				mynode.setCanRead(true);
			}else {
				if(readGroup != null && readGroup.size() > 0 && readGroup.contains(userId)) {
					mynode.setCanRead(true);
				}				
			}
		}
		
		// set write bit
		if(mynode.getWriteMembers() != null && mynode.getWriteMembers().size() > 0) {
			writeGroup = mynode.getWriteMembers();
			if(writeGroup.contains(userId)) {
				mynode.setCanWrite(true);
			}			
		}else {
			if(access == Access.ALLOW) {
				mynode.setCanWrite(true);
			}else {
				if(writeGroup != null && writeGroup.size() > 0 && writeGroup.contains(userId)) {
					mynode.setCanWrite(true);
				}				
			}
		}		
		
		
		
		/*
		if(mynode.getReadMembers() != null && mynode.getReadMembers().size() > 0) {
			readGroup = mynode.getReadMembers();
		}
		if(mynode.getWriteMembers() != null && mynode.getWriteMembers().size() > 0) {
			writeGroup = mynode.getWriteMembers();
		}
		if(readGroup != null && readGroup.size() > 0 && readGroup.contains(userId)) {
			mynode.setCanRead(true);
		}
		if(writeGroup != null && writeGroup.size() > 0 && writeGroup.contains(userId)) {
			mynode.setCanWrite(true);
		}
		*/
			
		
		// set permission bit for children
		if (node.hasChildren()) {
			for (TreeNode<MyNode> childNode : node.getChildren()) {
				setPermissionBits(userId, access, childNode, readGroup, writeGroup);
			}
		}		
		
	}
	
	
	/*
		walkTreePreOrder(myTree, new Stack<List<Integer>>(),
				(userId, treeNode, stack) -> {
					MyNode n = treeNode.getData();
					logger.info(n.toString());
					if(n.readMembers != null && n.readMembers.size() > 0){
						
					}
				});
	@FunctionalInterface
	private interface PermissionChecker<N> {

		public void checkPermission(Integer userId, TreeNode<N> node, Stack<List<Integer>> readStack) throws TreeNodeVisitException;
		
	}
	
	public static <N> void walkTreePreOrder(Integer userId, Tree<N> tree, Stack<List<Integer>> readStack, PermissionChecker<N> checker) throws TreeNodeVisitException {
		
		preOrderTraversal(userId, tree.getRootNode(), readStack, checker);
		
	}
	private static <N> void preOrderTraversal(Integer userId, TreeNode<N> node, Stack<List<Integer>> readStack, PermissionChecker<N> checker) throws TreeNodeVisitException {
		
		checker.checkPermission(userId, node, readStack);

		if (node.hasChildren()) {
			for (TreeNode<N> childNode : node.getChildren()) {
				preOrderTraversal(userId, childNode, readStack, checker);
			}
		}

	}
	*/
	
	private class MyNode extends Node {
		private int id = -1;
		private List<Integer> readMembers = null;
		private List<Integer> writeMembers = null;
		private boolean canRead = false;
		private boolean canWrite = false;
		public MyNode(int id, List<Integer> readMembers, List<Integer> writeMembers){
			this.id = id;
			this.readMembers = readMembers;
			this.writeMembers = writeMembers;
		}
		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}
		public List<Integer> getReadMembers() {
			return readMembers;
		}
		public void setReadMembers(List<Integer> readMembers) {
			this.readMembers = readMembers;
		}
		public List<Integer> getWriteMembers() {
			return writeMembers;
		}
		public void setWriteMembers(List<Integer> writeMembers) {
			this.writeMembers = writeMembers;
		}
		public boolean isCanRead() {
			return canRead;
		}
		public void setCanRead(boolean canRead) {
			this.canRead = canRead;
		}
		public boolean isCanWrite() {
			return canWrite;
		}
		public void setCanWrite(boolean canWrite) {
			this.canWrite = canWrite;
		}
		@Override
		public String toString() {
			return "MyNode [id=" + id + ", readMembers=" + readMembers + ", writeMembers=" + writeMembers + ", canRead="
					+ canRead + ", canWrite=" + canWrite + "]";
		}

	}	

}
