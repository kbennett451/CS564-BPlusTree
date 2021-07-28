import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
/**
 * B+Tree Structure
 * Key - StudentId
 * Leaf Node should contain [ key,recordId ]
 */
class BTree {

    /**
     * Pointer to the root node.
     */
    private BTreeNode root;
    /**
     * Number of key-value pairs allowed in the tree/the minimum degree of B+Tree
     **/
    private int t;

    BTree(int t) {
        this.root = null;
        this.t = t;
    }

    long search(long studentId) {
    	//start at root
    	BTreeNode curNode = root;
    	//init check that root is not null
    	if (curNode == null) {
    		return -1;
    	}
    	
    	BTreeNode foundNode = searchRecursive(curNode, studentId);
    	if (foundNode == null) {
    		return -1;
    	}
    	return foundNode.getValue(studentId);
    }
    private BTreeNode searchRecursive (BTreeNode node, long studentId) {
    	if (node == null) { // the leaf node has not been created yet
            return null;
        }
        if (node.leaf) { // we found an appropriate leaf node
            for (int i = 0; i <node.n; i++) {
            	if (studentId == node.keys[i]) {
            		return node;
            	}
            }
        }
        else { // this is an internal node
            for (int i = node.n; i >= 0; i--) {
            	if ((studentId >= node.keys[i]) && (node.children[i+1] != null) && (node.keys[i] > 0)) {
                	BTreeNode childNode = searchRecursive(node.children[i+1], studentId);
                    if (childNode == null) {
                    	return null;
                    }
                    else {
                        return childNode;
                    }
                }
            	else if ((studentId < node.keys[i]) && (i==0) && (node.children[i] != null)) {
                    BTreeNode childNode = searchRecursive(node.children[i], studentId);
                    if (childNode == null) {
                    	return null;
                    }
                    
                    else {
                        
                        return childNode;
                    }
                }
            }
        }
        
    	return null;
    }

    BTree insert(Student student) {
        /**
         * TODO: update student.csv
         */

         // find the correct leaf node
        BTreeNode node = root;

        if (node == null) { // this is if this is the firts entry
            root = insertRecursive(node, student);
            return this;
        }

        BTreeNode newNode = insertRecursive(node, student);
        if (newNode == null) { // if we inserted without a split
            return this;
        }
        // this will be the case if we have split the root and need to add a level to the tree
        this.root = new BTreeNode(this.t, false);
        this.root.children[0] = node;
        this.root.keys[0] = newNode.keys[0];
        this.root.children[1] = newNode;
        this.root.n++;
        
        return this;
    }

    /**
     * Recursive insert function to traverse the tree and add the new value where appropriate
     * @param node - the node to try the insert on
     * @param entry - the new entry
     * @return null if there is no split, the new node if we have created a new node that needs to be added to the parent.
     */
    private BTreeNode insertRecursive(BTreeNode node, Student entry) {
        if (node == null) { // the leaf node has not been created yet
            node = new BTreeNode(this.t, true);
            node.keys[0] = entry.studentId;
            node.values[0] = entry.recordId;
            node.n++;
            return node;
        }
        if (node.leaf) { // we found an appropriate leaf node
            if (node.hasKey(entry.studentId)) { // prevent the addition of duplicate keys
                return null;
            }
            else {
                this.addStudentToFile(entry);
            }
            if (node.hasSpace()) { // we can just put the key here
                for (int i = node.n; i >= 0; i--) { // loop backwards to shift all values right assuming that the entry will be stored somewhere to the left
                    if (entry.studentId < node.keys[i]) { // entry must be stored to the right of this key
                        node.keys[i+1] = node.keys[i];
                        node.values[i+1] = node.values[i];
                    }
                    else { // only hit this when the inserted value is the lowest in the node
                        node.keys[i] = entry.studentId;
                        node.values[i] = entry.recordId;
                        node.n++;
                        break; // TODO: make sure this is safe and we don't need to update the parent key in any circumstances
                    }
                }
            }
            else { // split
                return splitLeafNode(node, entry);
            }
        }
        else { // this is an internal node
            for (int i = 0; i < node.n; i++) {
                if (entry.studentId < node.keys[i]) {
                    BTreeNode newNode = insertRecursive(node.children[i], entry);
                    if (newNode == null) {
                        return null;
                    }
                    else {
                        // handle new internal node addition
                        newNode = handleAddInternalNode(node, newNode);
                        return newNode;
                    }
                }
                else if (i == node.n - 1) { // if we're here, we're looking at the outermost key in the node
                    BTreeNode newNode = insertRecursive(node.children[i+1], entry);
                    return (newNode == null) ? null : handleAddInternalNode(node, newNode);
                }
            }
        }
        return null;
    }

    /**
     * Takes the current internal node and adds a new internal node as a child
     * @param currNode - the node we are adding to
     * @param newNode - the new node that needs to be added
     * @return null if we don't need an additional split, otherwise a new node
     */
    private BTreeNode handleAddInternalNode(BTreeNode currNode, BTreeNode newNode) {
        long newKey = newNode.keys[0];
        if (currNode.n < currNode.maxKeys()) { // if there's space in the node
            for (int i = currNode.n; i >= 0; i--) { // shift keys
                if (newKey < currNode.keys[0]) {
                    currNode.keys[i+1] = currNode.keys[i];
                    currNode.children[i+1] = currNode.children[i];
                }
                else { // new key is greater than
                    currNode.keys[i] = newKey;
                    currNode.children[i + 1] = newNode;
                    currNode.n++;
                    break;
                }
            }
        }
        else { // no space, requires a split
            BTreeNode n2 = splitInternalNode(currNode);
            if (currNode == root) { // special handling for splitting the root
                this.root = new BTreeNode(this.t, false);
                currNode.n--;
                this.root.keys[0] = currNode.keys[currNode.n];
                currNode.keys[currNode.n] = 0;
                this.root.children[0] = currNode;
                this.root.children[1] = n2;
                this.root.n++;
                return null;
            }
            
            else { // if not the root, we can handle this normally with a recurrsive call
                /*
                BTreeNode temp = new BTreeNode(this.t, false);
                temp.keys[0] = currNode.keys[currNode.n];
                currNode.n--;
                temp.children[0] = currNode;
                temp.children[1] = n2;
                temp.n++;

                return temp;
                */
            
                handleAddInternalNode(n2, newNode);
                return handleAddInternalNode(currNode, n2);
                
            }
        } 
        return null;
    }

    /**
     * Handle splitting an internal node
     * @param currNode - the node that needs to be split
     * @return a reference to the new node that was created
     */
    private BTreeNode splitInternalNode(BTreeNode currNode) {
        BTreeNode newNode = new BTreeNode(this.t, false);
        int newIndex = 0; // just instantiating this for use later
        for (int i = currNode.getMidpointIndex(); i < currNode.maxKeys(); i++) {
            // new index should start at 0 so the difference between the max and  the current second half index
            newIndex = i - currNode.getMidpointIndex(); 
            newNode.keys[newIndex] = currNode.keys[i]; // move second half into first half of the new node
            newNode.children[newIndex] = currNode.children[i]; // move second half into first half of the new node
            if (i == currNode.maxKeys() - 1) {
                newNode.children[newIndex + 1] = currNode.children[i + 1];
                currNode.children[i + 1] = null;
            }
            
            newNode.n++; // increment new node
            
            // remove values from current node
            currNode.keys[i] = 0;
            currNode.children[i] = null;
            currNode.n--; // decrement current node
        }
        return newNode;
    }
  
    /**
     * Handle the splitting of a leaf node
     * @param node - the node to split
     * @param entry - the entry to add to the new split
     * @return a reference to the new node that is created
     */
    private BTreeNode splitLeafNode(BTreeNode node, Student entry) {
        BTreeNode newNode = new BTreeNode(this.t, true);
        int newIndex = 0;
        for (int i = node.getMidpointIndex(); i < node.maxKeys(); i++) {
            newIndex = i - node.getMidpointIndex();
            newNode.keys[newIndex] = node.keys[i];
            newNode.values[newIndex] = node.values[i];
            
            newNode.n++; // increment new node
            
            // remove values from current node
            node.keys[i] = 0;
            node.values[i] = 0;
            node.n--; // decrement current node
        }
        // move sibling pointers
        newNode.next = (node.next == null) ? null : node.next.next;
        node.next = newNode;

        // insert the new value
        newNode.keys[newNode.n] = entry.studentId;
        newNode.values[newNode.n] = entry.recordId;
        newNode.n++;
        
        return newNode;
    }


    /**
     * Adds the student entry to the file
     * @param student - the student to add
     */
    private void addStudentToFile(Student student) {
        if (existsInFile(student)) {
            return;
        }
        PrintWriter pw = null;
        String output = "";
        output += Long.toString(student.studentId) + ",";
        output += student.studentName + ",";
        output += student.major + ",";
        output += student.level + ",";
        output += Integer.toString(student.age) + ",";
        output += Long.toString(student.recordId);
        try {
            pw = new PrintWriter(new FileOutputStream(new File("src/Student.csv"), true));
            pw.println(output);
            pw.close();
        } catch (FileNotFoundException e) {
            System.out.println("File not found.");
        }
    }

    private boolean existsInFile(Student student) {
        Scanner scan = null;
        try {
            scan = new Scanner(new File("src/Student.csv"));
            while(scan.hasNextLine()) {
                if (Long.parseLong(scan.nextLine().split(",")[0]) == student.studentId) {
                    scan.close();
                    return true;
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("File not found.");
        }
        return false;

    private BTreeNode findParentNode (BTreeNode node, BTreeNode parentNode, long studentId) {
    	if (node == null) { // the leaf node has not been created yet
            return null;
        }
        if (node.leaf) { // we found an appropriate leaf node
            for (int i = 0; i <node.n; i++) {
            	if (studentId == node.keys[i]) {
            		return parentNode;
            	}
            }
        }
        else { // this is an internal node
            for (int i = node.n; i >= 0; i--) {
            	if ((studentId >= node.keys[i]) && (node.children[i+1] != null) && (node.keys[i] > 0)) {
                	BTreeNode childNode = findParentNode(node.children[i+1], node, studentId);
                    if (childNode == null) {
                    	return null;
                    }
                    else {
                        return childNode;
                    }
                }
            	else if ((studentId < node.keys[i]) && (i==0) && (node.children[i] != null)) {
                    BTreeNode childNode = findParentNode(node.children[i], node, studentId);
                    if (childNode == null) {
                    	return null;
                    }
                    
                    else {
                        
                        return childNode;
                    }
                }
            }
        }
        
    	return null;
    }
    
    private int parentToChildKey (BTreeNode parentNode, BTreeNode node, long value) {
    	int childKey=0;
    	for (int i = parentNode.n; i >= 0; i--) {
        	if (value >= parentNode.keys[i] && parentNode.keys[i] >0) {
            	return i+1;
        	}
    	}
    	return childKey;
    }
    
    private boolean canRedistribute (BTreeNode parentNode, int currChild, int sibling) {
    	int joinedKeyCount = parentNode.children[currChild].n + parentNode.children[sibling].n;
    	//int joinedMinKeyCnt = (parentNode.children[currChild].t)/2 +1 + ((parentNode.children[sibling].t)/2) + 1;
    	int joinedMinKeyCnt = (parentNode.children[currChild].t)-1 + ((parentNode.children[sibling].t) -1 );
    	//System.out.println(parentNode.children[3].keys[2] +"|" + parentNode.children[currChild].n + "|" + parentNode.children[sibling].n + "|joined key count: " + joinedKeyCount + "|joinedMinKeyCnt: " + joinedMinKeyCnt);
    	if (joinedKeyCount<= joinedMinKeyCnt) {
    		return false;
    	}
    	return true;
    }
    private void balance(BTreeNode parentNode, int currChild, int sibling) {
    	int leftNode,rightNode=0;
    	if (currChild>sibling) {
    		leftNode=sibling;
    		rightNode=currChild;
    	}
    	else {
    		leftNode=currChild;
    		rightNode=sibling;
    	}
    	if (currChild > sibling) {  //node where we did removal is to the right
    		//BTreeNode copy = parentNode.children[currChild];
    		//parentNode.children[currChild].clearNode();
    		int i,j = 0 ;
    		int sibStart=(parentNode.children[sibling].t-1);
    		int sibMax=parentNode.children[sibling].n;
    		int offSet = sibMax-sibStart;
    		int currNum=parentNode.children[currChild].n;
    		
    		for (i=0; i<currNum; i++) {
    			
    			parentNode.children[currChild].keys[i+offSet] = parentNode.children[currChild].keys[i];
    			parentNode.children[currChild].values[i+offSet] = parentNode.children[currChild].values[i];
    			//parentNode.children[currChild].n++;
    		}
    		for (i=sibStart; i<sibMax; i++, j++) {
    			parentNode.children[currChild].keys[j] = parentNode.children[sibling].keys[i];
    			parentNode.children[currChild].values[j] = parentNode.children[sibling].values[i];
    			//parentNode.children[currChild].n++;
    			parentNode.children[sibling].keys[i]=0;
    			parentNode.children[sibling].values[i]=0;
    		}
    		parentNode.children[currChild].n=currNum+offSet;
    		//update parent node key
    		parentNode.keys[currChild-1]=parentNode.children[currChild].keys[0];
    		parentNode.children[sibling].n=sibStart;
    		
    	}
    	else {//currChild on LEFT and is leftNode
    		int i,j = 0 ;
    		int shrinkStart=(parentNode.children[leftNode].n);
    		int shrinkMax=(parentNode.children[leftNode].t-1);
    		int sibMax=parentNode.children[rightNode].n;
    		int offSet = shrinkMax-shrinkStart;
    		int currNum=parentNode.children[rightNode].n;
    		//int shrinkCurrNum=parentNode.children[leftNode].n;
    		int preOffSet,postOffSet = 0;
    		if (currChild==rightNode) {
    			preOffSet=offSet;
    		}
    		else {
    			postOffSet=offSet;
    		}

    		System.out.println("BALANCE BEF: "+ parentNode.children[leftNode].n + "|" + shrinkStart + "|" + sibMax + "|" + currNum);
    		for (i=shrinkStart; i<shrinkMax; i++, j++) {
    			parentNode.children[leftNode].keys[i] = parentNode.children[rightNode].keys[j];
    			parentNode.children[leftNode].values[i] = parentNode.children[rightNode].values[j];
    		}
    		for (i=offSet; i<currNum; i++) {
    			
    			parentNode.children[rightNode].keys[i-offSet] = parentNode.children[rightNode].keys[i];
    			parentNode.children[rightNode].values[i-offSet] = parentNode.children[rightNode].values[i];
    			//parentNode.children[leftNode].n++;
    		}
    		for (i=(currNum-offSet); i<currNum; i++) {
    			
    			parentNode.children[rightNode].keys[i] = 0; 
    			parentNode.children[rightNode].values[i] = 0;
    			//parentNode.children[leftNode].n++;
    		}
    		parentNode.children[leftNode].n=shrinkMax;
    		//update parent node key
    		parentNode.keys[rightNode-1]=parentNode.children[rightNode].keys[0];
    		parentNode.children[rightNode].n=currNum-offSet;
    	}
    }
    boolean mergeNodesOnDelete(BTreeNode parentNode, int currChild, int sibling) {
    	int smaller=0,larger=0;
    	if (currChild>sibling) {
    		smaller=sibling;
    		larger=currChild;
    	}
    	else {
    		smaller=currChild;
    		larger=sibling;
    	}
    	//merge keys:
    	long oldKey = parentNode.keys[1];
		int smallerNum=parentNode.children[smaller].n;
		int largerNum=parentNode.children[larger].n;
		
		for (int i=0; i<largerNum; i++) {
			parentNode.children[smaller].keys[i+smallerNum] = parentNode.children[larger].keys[i];
			if (parentNode.children[smaller].leaf) {
				parentNode.children[smaller].values[i+smallerNum] = parentNode.children[larger].values[i];
			}
		}
		parentNode.children[larger].clearNode();
		parentNode.children[larger]=parentNode.children[larger+1]; //remove?
		parentNode.children[smaller].n=smallerNum+largerNum;
    	//handle action on parent node
    	if (smaller>0 ) {
    		
    		parentNode.keys[smaller]=parentNode.children[smaller+1].keys[0];
    	}
    	else if (larger<2) {
    		//propogateKeyUp(parentNode,parentNode.children[1],parentNode.keys[0]); broken
    	}
    	//shift parent nodes left
    	if (larger<parentNode.n) {
	    	for (int i=larger; i<parentNode.n; i++) {
				parentNode.keys[i-1] = parentNode.keys[i];
				parentNode.children[i-1] = parentNode.children[i];
			}
	    	if (smaller==0) {
	    		//BTreeNode grandParent = findParentNode(parentNode,parentNode,parentNode.keys[0]); broken for now
	    		//propogateKeyUp(grandParent,parentNode,oldKey);
	    	}
    	}
    	
    	//make last node 0 and reduce count
    	parentNode.keys[(parentNode.n)-1]= 0;
    	parentNode.children[parentNode.n] = null;
    	parentNode.n--;
		
    	return true;

    }

    boolean delete(long studentId) {
    	BTreeNode curNode = root;
    	//init check that root is not null
    	if (curNode == null) {
    		return false;
    	}
    	
    	BTreeNode foundNode = searchRecursive(curNode, studentId);
    	BTreeNode parent = findParentNode(curNode,curNode,studentId);
    	if (foundNode == null) {
    		return false;
    	}
    	
    	//remove node then do the rest
    	if ( foundNode.removeValue(studentId) == false) {return false;}
    	
    	deleteRecursive(parent,foundNode,studentId,studentId);
    	if (foundNode == root && !root.leaf && root.n == 0) {
            root = root.children[0];
    	}
    	
    	return true;
    }
    boolean deleteRecursive(BTreeNode parent, BTreeNode foundNode, long studentId, long origKey) {
    	if (foundNode == root) {
    		return false;
    	}
    	boolean needsRebalance = false;
    	int keyForVal = parent.keyForValue(origKey);
    	if (!foundNode.hasMinPopulation()) {  //not populated enough, need to rebalance or merge
    		int childKey = parentToChildKey(parent, foundNode,studentId);
    		//balance (parent, childKey, childKey-1); //TODO: delete
    		
    		if (childKey<1 && parent.n>1 ) {//left-most so need to try with right sibling
    			if (canRedistribute(parent, childKey, childKey+1)) {
    				balance(parent,childKey,childKey+1);
    			}
    			else {
    				mergeNodesOnDelete (parent, childKey, childKey+1);
    				needsRebalance=true;
    			}
    		}
    		else if (childKey < parent.n && parent.keys[childKey+1]>0) {  //has right sibling, try first
    			if (canRedistribute(parent, childKey, childKey+1)) {
    				balance (parent, childKey, childKey+1);
    			}
    			else if (canRedistribute(parent, childKey, childKey-1)) {
    				balance (parent, childKey, childKey-1);
    			}
    			else {//merge with right by default if inner
    				mergeNodesOnDelete (parent, childKey, childKey+1);
    				needsRebalance=true;
    			}
    		}
    		else {
    			//right-most so must try left
    			if (canRedistribute(parent, childKey, childKey-1)) {
    				balance (parent, childKey, childKey-1);
    			}
    			else {
    				mergeNodesOnDelete (parent, childKey, childKey-1);
    				needsRebalance=true;
    			}
    		}
    	}
    	else if(keyForVal>-1) { 

    		propogateKeyUp(parent,foundNode,studentId);
    	}
    	if (parent != root && parent.n<parent.t && needsRebalance) {
    		BTreeNode grandParent = findParentNode(parent,parent,parent.keys[0]);
    		return deleteRecursive(grandParent,parent,parent.keys[0],origKey);
    	}
    	return true;
    }
    private void propogateKeyUp(BTreeNode parent, BTreeNode node, long key) {
    	//needs to be fixed- 
    	
    	int keyForVal = node.keyForValue(key);
    	if(keyForVal>-1) { 
    		parent.keys[keyForVal] = node.keys[0]; //set to min value from found Node
    		if (keyForVal==1) {
    			BTreeNode grandParent = findParentNode(parent,parent,parent.keys[0]);
    			propogateKeyUp(grandParent, parent, parent.keys[0]);
    		}
    	}
    }

    /**
     * Returns a list of the record IDs in the leaf nodes of the tree
     * @return - a list of the record IDs from left to right in the tree
     */
    List<Long> print() {

        List<Long> listOfRecordID = new ArrayList<>();

        BTreeNode node = root;
        while (!node.leaf) { // find the left most leaf node
            node = node.children[0];
        }
        
        while (node != null) { // travers the linked-list like structure of the leaf nodes
            for (int i = 0; i < node.n; i++) {
                listOfRecordID.add(node.values[i]);
            }
            node = node.next;
        };
        return listOfRecordID;
    }

    /**
     * For testing purposes
     * Prints the tree nicely for debugging
     *
     * TODO: Delete Me
     */
    void printTree() {
        ArrayList<BTreeNode> nodeList = new ArrayList<BTreeNode>();
        nodeList.add(this.root);
        while (!nodeList.isEmpty()) {
            // print keys of the whole list
            String out = "";
            for (int i = 0; i < nodeList.size(); i++) {
                out += printNode(nodeList.get(i));
            }
            // add all children of the keys
            int size = nodeList.size();
            for (int j = 0; j < size; j++) {
                BTreeNode node = nodeList.get(0);
                for (int k = 0; k <= node.n; k++) {
                    if (node.children[k] != null) {
                        nodeList.add(node.children[k]);
                    }
                }
                nodeList.remove(node);
            }
            if (nodeList.size() > 0) {
                for (int i = 0; i < 10 - nodeList.size(); i++) {
                    out = "\t" + out;
                }
            }
            System.out.println(out);
        }
    }

    /**
     * For testing purposes, returns a string of the node
     * @param node - node to print
     * @return String representing the node to print
     * 
     * TODO: Delete Me
     */
    String printNode(BTreeNode node) {
        String out = "";
        if (node.leaf) {
            for (int i = 0; i < node.maxKeys(); i++) {
                if (node.keys[i] == 0) { break; }
                out += "(" + node.keys[i] + ")";
            }
        }
        else {
            for (int i = 0; i < node.maxKeys(); i++) {
                //if (node.keys[i] == 0) { break; }
                if (i == 0) {
                    out += "<" + node.children[i] + ">";
                }
                out += node.keys[i];
                if (i < node.maxKeys()) {
                    out += "<" + node.children[i+1] + ">";
                }
                out += (i < node.maxKeys() - 1 ? " " : "");
            }
        }
        return "[" + out + "] ";
    }
}
