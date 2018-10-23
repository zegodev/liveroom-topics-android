package com.zego.playground.demo.entity;

/**
 * Created by zego on 2018/10/16.
 */

public class ModuleInfo {

    private String module;

    public String getModule() {
        return module;
    }

    public ModuleInfo moduleName(String module) {
        this.module = module;
        return this;
    }

}
