package com.zego.liveroomplayground.demo.entity;

/**
 * Created by zego on 2018/10/16.
 */

public class ModuleInfo {

    private String module;
    private String titleName;

    public String getTitleName() {
        return titleName;
    }

    public String getModule() {
        return module;
    }

    public ModuleInfo moduleName(String module) {
        this.module = module;
        return this;
    }


    public ModuleInfo titleName(String titleName) {
        this.titleName = titleName;
        return this;
    }


}
