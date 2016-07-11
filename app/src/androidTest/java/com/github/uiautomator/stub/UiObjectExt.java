package com.github.uiautomator.stub;

/**
 + * MOLLARD Rémi / ROYER Alexis
 + * android-uiautomator-server: "parent" selector addition, reflexive implementations
 + */

import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiCollection;
import android.support.test.uiautomator.UiSelector;
import android.view.accessibility.AccessibilityNodeInfo;
import android.support.test.uiautomator.Configurator;
/**
 * This class used to extended UiObject with findAccessibilityNodeInfo method.
 * Then use it to find a view parent.
 */
public class UiObjectExt extends UiObject {
    public UiObjectExt() {
        super(null);
        }

    /**
     * get accessibility node informations for a UiObject.
     *
     * @param uiObject uiobject need to find for get NodeInfo.
     * @return AccessibilityNodeInfo The AccessibilityNodeInformations for uiObject parameter
     * @throws Exception
     */
    public AccessibilityNodeInfo getAccessibilityNodeInfo(UiObject uiObject) throws Exception {
        // Use reflexive programming to call the "findAccessibilityNodeInfo" method
        java.lang.reflect.Method findAccessibilityNodeInfoMethod = UiObject.class.getDeclaredMethod("findAccessibilityNodeInfo", long.class);
        findAccessibilityNodeInfoMethod.setAccessible(true);
        AccessibilityNodeInfo node = (
                (AccessibilityNodeInfo)
                         findAccessibilityNodeInfoMethod.invoke(
                         uiObject, Configurator.getInstance().getWaitForSelectorTimeout()
                         )
        );
        return node;
        }

    /**
     * Get parent for a UiObject. First use getParent function on AccessibilityNodeInfo to find
     * AccessibilityNodeInfo parent of parameter. After that find UiObject match with that
     * AccessibilityNodeInfo.
     *
     * @param uiObjectToFindParent uiobject were need parent.
     * @return UiObject The Uiobject reprenst Parent for UiObject parameter
     * @throws Exception
     */
    public UiObject GetParentForuiObject(UiObject uiObjectToFindParent) throws Exception {
        AccessibilityNodeInfo currentNodeInfo = getAccessibilityNodeInfo(uiObjectToFindParent);
        AccessibilityNodeInfo parentNodeInfo = null;
        UiObject parentView = null;
        if (currentNodeInfo != null) {
            parentNodeInfo = currentNodeInfo.getParent();
            if (parentNodeInfo != null) {
                parentView = getuiObjectFromNodeInfo(parentNodeInfo);
                if (parentView != null) {
                    return parentView;
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Get UiObject from a AccessibilityNodeInfo. This function loops over all children with the same
     * classname as uiObjectToFindParent and check if AccessibilityNodeInfo match with AccessibilityNodeInfo
     * from parameter.
     *
     * @param nodeInfoToFind AccessibilityNodeInfo wich need find UiObject
     * @return UiObject The good UiObject from AccessibilityNodeInfo parameter
     * @throws Exception
     */
    public UiObject getuiObjectFromNodeInfo(AccessibilityNodeInfo nodeInfoToFind) throws Exception {
        // Create a new uiObject collection
        UiCollection collectNodes = new UiCollection(new UiSelector());
        AccessibilityNodeInfo aChildNodeInfo = null;

        // Create a new selector to find root children that match with parameter classname
        UiSelector s = new UiSelector().className(nodeInfoToFind.getClassName().toString());
        int childCount = collectNodes.getChildCount(s);

        // Loop over all children to find the UiObject that matches with the parameters
        for (int i = 0; i< childCount;i++) {
            UiObject aChild = collectNodes.getChildByInstance(s, i);
            aChildNodeInfo = getAccessibilityNodeInfo(aChild);
            // For each children, get AccessibilityNodeInfo and compare unique id value (NodeInfo@) with unique id from parameter
            boolean delta = compareNodeInfo(aChildNodeInfo, nodeInfoToFind);
            if (delta) {
                return aChild;
            }
        }
        return null;
    }

    /**
     * Node info comparison.
     *
     * @param nodeInfo1,nodeInfo2 two accessibilityNodeInfo to compare.
     * @return boolean True if same AccessibilityNodeInfo, False otherwise.
     */
    public boolean compareNodeInfo(AccessibilityNodeInfo nodeInfo1,AccessibilityNodeInfo nodeInfo2) {
        String nodeId1 = getIdInNodeInfoString(nodeInfo1.toString());
        String nodeId2 = getIdInNodeInfoString(nodeInfo2.toString());
        if ((nodeId1 != null) && (nodeId2 != null)) {
            return nodeId1.equals(nodeId2);
        } else {
            return false;
        }
    }

    /**
     +     * getIdInNodeInfoString. This function find unique ID (NodeInfo@) in a AccessibilityNodeInfo String
     +     * and return them.
     +     *
     +     * @param nodeInfo String AccessibilityNodeInfo
     +     * @return String unique ID for AccessiblityNodeInfo parameter
     +     */
    private String getIdInNodeInfoString(String nodeInfo) {
        int indexStart = 0;
        int indexEnd = 0;
        if ((indexStart = nodeInfo.indexOf("NodeInfo@")) != -1) {
            indexStart = indexStart+9;
            indexEnd = nodeInfo.indexOf(";");
            return nodeInfo.substring(indexStart, indexEnd);
        } else {
            return null;
        }
    }
}
