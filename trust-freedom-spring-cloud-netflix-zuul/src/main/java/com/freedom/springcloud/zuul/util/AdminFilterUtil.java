package com.freedom.springcloud.zuul.util;


import com.freedom.springcloud.zuul.common.FilterInfo;
import com.freedom.springcloud.zuul.filters.FilterScriptManagerServlet;

/**
 * Utility method to build form data for the Admin page for uploading and downloading filters
 * 
 */
public class AdminFilterUtil {

    public static String getState(FilterInfo filter) {
        String state = "inactive";
        if(filter.isActive())state = "active";
        if(filter.isCanary())state = "canary";
        return state;

    }

    /**
     * 构造停用的链接
     * @param filter_id
     * @param revision
     * @return
     */
    public static String buildDeactivateForm(String filter_id, int revision) {
        if (FilterScriptManagerServlet.adminEnabled.get()) {
            return "<form  method=\"POST\" action=\"scriptmanager?action=DEACTIVATE&filter_id=" + filter_id + "&revision=" + revision + "\" >\n" +
                   "<input type=\"submit\" value=\"deactivate\"/></form>";
        } else {
            return "";
        }
    }

    /**
     * 构造启用的链接
     * @param filter_id
     * @param revision
     * @return
     */
    public static String buildActivateForm(String filter_id, int revision) {
        if (FilterScriptManagerServlet.adminEnabled.get()) {
            return "<form  method=\"POST\" action=\"scriptmanager?action=ACTIVATE&filter_id=" + filter_id + "&revision=" + revision + "\" >\n" +
                   "<input type=\"submit\" value=\"activate\"/></form>";
        } else {
            return "";
        }
    }

    /**
     * 构造置为金丝雀版本的链接
     * @param filter_id
     * @param revision
     * @return
     */
    public static String buildCanaryForm(String filter_id, int revision) {
        if (FilterScriptManagerServlet.adminEnabled.get()) {
            return "<form  method=\"POST\" action=\"scriptmanager?action=CANARY&filter_id=" + filter_id + "&revision=" + revision + "\" >\n" +
                   "<input type=\"submit\" value=\"canary\"/></form>";
        } else {
            return "";
        }
    }

    /**
     * 构造下载链接
     * @param filter_id
     * @param revision
     * @return
     */
    public static String buildDownloadLink(String filter_id, int revision) {
        return "<a href=scriptmanager?action=DOWNLOAD&filter_id=" + filter_id + "&revision=" + revision + ">DOWNLOAD</a>";
    }

}
