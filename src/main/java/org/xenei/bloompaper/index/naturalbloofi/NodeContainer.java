package org.xenei.bloompaper.index.naturalbloofi;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class NodeContainer {

    protected SortedSet<Node> children;
    private NodeContainer parent;

    protected NodeContainer(NodeContainer parent, SortedSet<Node> children) {
        this.parent = parent;
        this.children = children;
    }

    public NodeContainer getParent() {
        return parent;
    }

    public void setParent(NodeContainer parent) {
        this.parent = parent;
    }

    final boolean hasChildren() {
        return children != null && !children.isEmpty();
    }

    final void searchChildren(Node head, Consumer<Node> consumer) {
        if (hasChildren()) {
            children.tailSet(head).forEach(consumer);
        }
    }

    final void forChildren(Consumer<Node> consumer) {
        if (hasChildren()) {
            children.forEach(consumer);
        }
    }

    final void addChild(Node node) {
        if (children == null) {
            synchronized (this) {
                if (children == null) {
                    children = new TreeSet<Node>();
                }
            }
        }
        List<Node> nodeChildren = new ArrayList<Node>();
        for (Node n : children) {
            if (node.contains(n)) {
                if (node.equals(n)) {
                    n.getIds().addAll(node.getIds());
                    return;
                }
                nodeChildren.add(n);
            }
            if (n.contains(node)) {
                n.addChild(node);
                return;
            }
        }
        if (!nodeChildren.isEmpty()) {
            nodeChildren.forEach(node::addChild);
            children.removeAll(nodeChildren);
        }
        node.setParent(this);
        children.add(node);
    }

    abstract public int getId();

    final void removeChild(Node child) {
        children.remove(child);
        child.forChildren(this::addChild);
    }

    final boolean testChildren(Node head, Predicate<Node> consumer) {
        if (hasChildren()) {
            for (Node child : children.tailSet(head)) {
                if (!consumer.test(child)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (parent != null) {
            sb.append(parent).append(" -> ");
        }
        sb.append(getId());
        if (hasChildren()) {
            sb.append(" *");
        }
        return sb.toString();
    }
}