package com.emilsoft.multileveladapter.task;

import com.emilsoft.multileveladapter.callable.AdapterCallable;
import com.emilsoft.multileveladapter.model.MultiLevelItem;

import java.util.ArrayList;
import java.util.List;

public class AddItemTask<T extends MultiLevelItem<?, T>> implements AdapterCallable<T> {

    private final ItemProcessedListener<T> listener;
    private final List<T> items;
    private final T item;

    public AddItemTask(List<T> items, T item, ItemProcessedListener<T> listener) {
        this.items = items;
        this.item = item;
        this.listener = listener;
    }

    @Override
    public void onComplete(T result) {
        if(listener != null)
            listener.onItemProcessed(result);
    }

    @Override
    public T call() throws Exception {
        T parent = item.getParent();
        int itemIndex = items.indexOf(item);
        int index = items.indexOf(parent);
        if(itemIndex != -1) {
            //The item is visible and items contains item
            update(items, item, itemIndex);
            // if index != -1 the item is child of another item
            // otherwise it's a top-item
            if(index != -1) {
                T parentInstance = items.get(index);
                item.setParent(parentInstance);
            }
        } else {
            //This is a new fresh item or a collapsed item
            if(index != -1) {
                T parentInstance = items.get(index);
                item.setParent(parentInstance);
                // if the parent is collapsed, then this item is collapsed but the parent is not
                // otherwise also this item is not collapsed and it's child of another item
                if(parentInstance.isCollapsed()) {
                    if (!parentInstance.hasChildren()) {
                        parentInstance.setChildren(new ArrayList<T>());
                    }
                    int i = parentInstance.getChildren().indexOf(item);
                    if(i != -1) {
                        update(parentInstance.getChildren(), item, i);
                        parentInstance.getChildren().set(i, item);
                    } else {
                        item.setLevel(parentInstance.getLevel() + 1);
                        parentInstance.getChildren().add(item);
                    }
                    return null;
                }
            } else {
                int parentIndex = -1;
                int visibleCommentIndex = 0;
                boolean parentFound = false;
                if(parent != null) {
                    while (visibleCommentIndex < items.size() && !parentFound) {
                        T visibleComment = items.get(visibleCommentIndex);
                        if (visibleComment.hasChildren() && (parentIndex = visibleComment.getChildren().indexOf(parent)) != -1)
                            parentFound = true;
                        visibleCommentIndex++;
                    }
                }
                // if parent is found, then the item is collapsed
                // otherwise it's a fresh new top item (doesn't have a parent and is not contained in items)
                if(parentFound) {
                    T visibleComment = items.get(visibleCommentIndex - 1);
                    T parentInstance = visibleComment.getChildren().get(parentIndex);
                    item.setParent(parentInstance);
                    if(parentInstance.isCollapsed()) {
                        // parent has collapsed children
                        if(!parentInstance.hasChildren()) {
                            parentInstance.setChildren(new ArrayList<T>());
                        }
                        int i = parentInstance.getChildren().indexOf(item);
                        if(i != -1) {
                            update(parentInstance.getChildren(), item, i);
                            parentInstance.getChildren().set(i, item);
                        } else {
                            item.setLevel(parentInstance.getLevel() + 1);
                            parentInstance.getChildren().add(item);
                        }
                    }
                    // maintain coherency with onCollapse
                    int i = visibleComment.getChildren().indexOf(item);
                    if(i != -1) {
                        update(visibleComment.getChildren(), item, i);
                        visibleComment.getChildren().set(i, item);
                    } else {
                        item.setLevel(parentInstance.getLevel() + 1);
                        visibleComment.getChildren().add(parentIndex + 1, item);
                    }
                    //the item is collapsed and so not visible
                    return null;
                }
            }
        }
        if(item.getParent() != null) {
            item.setLevel(item.getParent().getLevel() + 1);
        } else {
            item.setLevel(1);
        }
        return item;
    }

    private static <T extends MultiLevelItem<?, T>> void update(List<T> items, T item, int index) {
        T oldItem = items.get(index);
        item.setLevel(oldItem.getLevel());
        item.setIsCollapsed(oldItem.isCollapsed());
        item.setChildren(oldItem.getChildren());
    }

    public interface ItemProcessedListener<T> {

        void onItemProcessed(T multiLevelItem);

    }

}
