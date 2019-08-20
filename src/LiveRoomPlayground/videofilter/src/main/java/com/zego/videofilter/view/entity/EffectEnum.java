package com.zego.videofilter.view.entity;

import com.zego.videofilter.R;
import com.zego.videofilter.faceunity.entity.Effect;

import java.util.ArrayList;

/**
 * Created by tujh on 2018/1/30.
 */
public enum EffectEnum {
    /**
     * 关闭道具
     */
    EffectNone("none", R.drawable.ic_delete_all, "none", 1, Effect.EFFECT_TYPE_NONE, 0),
    /**
     * 道具贴纸
     */
    Effect_background("背景分离", R.drawable.background_test, "normal/background_test.bundle", 4, Effect.EFFECT_TYPE_NORMAL, 0),
    Effect_bling("亮晶晶", R.drawable.bling, "normal/bling.bundle", 4, Effect.EFFECT_TYPE_NORMAL, 0),
    Effect_fengya_ztt_fu("风雅", R.drawable.fengya_ztt_fu, "normal/fengya_ztt_fu.bundle", 4, Effect.EFFECT_TYPE_NORMAL, 0),
    Effect_hudie_lm_fu("蝴蝶", R.drawable.hudie_lm_fu, "normal/hudie_lm_fu.bundle", 4, Effect.EFFECT_TYPE_NORMAL, 0),
    Effect_touhua_ztt_fu("头花", R.drawable.touhua_ztt_fu, "normal/touhua_ztt_fu.bundle", 4, Effect.EFFECT_TYPE_NORMAL, 0),
    Effect_juanhuzi_lm_fu("卷胡子", R.drawable.juanhuzi_lm_fu, "normal/juanhuzi_lm_fu.bundle", 4, Effect.EFFECT_TYPE_NORMAL, 0),
    Effect_mask_hat("面具帽", R.drawable.mask_hat, "normal/mask_hat.bundle", 4, Effect.EFFECT_TYPE_NORMAL, 0),
    Effect_yazui("鸭嘴", R.drawable.yazui, "normal/yazui.bundle", 4, Effect.EFFECT_TYPE_NORMAL, 0),
    Effect_yuguan("羽冠", R.drawable.yuguan, "normal/yuguan.bundle", 4, Effect.EFFECT_TYPE_NORMAL, 0);

    private String bundleName;
    private int resId;
    private String path;
    private int maxFace;
    private int effectType;
    private int description;

    EffectEnum(String name, int resId, String path, int maxFace, int effectType, int description) {
        this.bundleName = name;
        this.resId = resId;
        this.path = path;
        this.maxFace = maxFace;
        this.effectType = effectType;
        this.description = description;
    }

    public String bundleName() {
        return bundleName;
    }

    public int resId() {
        return resId;
    }

    public String path() {
        return path;
    }

    public int maxFace() {
        return maxFace;
    }

    public int effectType() {
        return effectType;
    }

    public int description() {
        return description;
    }

    public Effect effect() {
        return new Effect(bundleName, resId, path, maxFace, effectType, description);
    }

    public static ArrayList<Effect> getEffectsByEffectType(int effectType) {
        ArrayList<Effect> effects = new ArrayList<>(16);
        effects.add(EffectNone.effect());
        for (EffectEnum e : EffectEnum.values()) {
            if (e.effectType == effectType) {
                effects.add(e.effect());
            }
        }
        return effects;
    }
}
