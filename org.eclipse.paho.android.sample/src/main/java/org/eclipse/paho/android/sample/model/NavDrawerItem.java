package org.eclipse.paho.android.sample.model;


import org.eclipse.paho.android.sample.activity.Connection;

public class NavDrawerItem {
    private boolean showNotify;
    private String title;
    private String subTitle;
    private String handle;

    public NavDrawerItem(){

    }

    public NavDrawerItem(Connection connection){
        this.title = connection.getId();
        this.subTitle = connection.getHostName();
        this.handle = connection.handle();
        if(!connection.isConnected()){

        }
    }

    public NavDrawerItem(boolean showNotify, String title){
        this.showNotify = showNotify;
        this.title = title;
    }

    public boolean isShowNotify(){
        return showNotify;
    }

    public void setShowNotify(boolean showNotify){
        this.showNotify = showNotify;
    }

    public String getTitle(){
        return title;
    }

    public void setTitle(String title){
        this.title = title;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

}
