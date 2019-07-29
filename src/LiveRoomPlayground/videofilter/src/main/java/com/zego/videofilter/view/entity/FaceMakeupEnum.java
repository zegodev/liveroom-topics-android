package com.zego.videofilter.view.entity;

import android.support.v4.util.Pair;
import android.util.SparseArray;

import com.zego.videofilter.R;
import com.zego.videofilter.faceunity.entity.FaceMakeup;
import com.zego.videofilter.faceunity.entity.Filter;
import com.zego.videofilter.faceunity.entity.MakeupItem;

import java.util.ArrayList;
import java.util.List;

/**
 * @author LiuQiang on 2018.11.12
 * 口红用 JSON 表示，其他都是图片
 */
public enum FaceMakeupEnum {

    /**
     * 美妆项，前几项是预置的效果
     * 排在列表最前方，顺序为桃花妆、雀斑妆、朋克妆（其中朋克没有腮红，3个妆容的眼线、眼睫毛共用1个的）
     */
    MAKEUP_NONE("卸妆", "", FaceMakeup.FACE_MAKEUP_TYPE_NONE, R.drawable.makeup_none_normal, R.string.makeup_radio_remove, true),

    // 腮红
    MAKEUP_BLUSHER_01("MAKEUP_BLUSHER_01", "blusher/mu_blush_01.png", FaceMakeup.FACE_MAKEUP_TYPE_BLUSHER, R.drawable.demo_blush_01, R.string.makeup_radio_blusher, false),
    MAKEUP_BLUSHER_18("MAKEUP_BLUSHER_18", "blusher/mu_blush_18.png", FaceMakeup.FACE_MAKEUP_TYPE_BLUSHER, R.drawable.demo_blush_18, R.string.makeup_radio_blusher, true),
    MAKEUP_BLUSHER_19("MAKEUP_BLUSHER_19", "blusher/mu_blush_19.png", FaceMakeup.FACE_MAKEUP_TYPE_BLUSHER, R.drawable.demo_blush_19, R.string.makeup_radio_blusher, true),
    MAKEUP_BLUSHER_20("MAKEUP_BLUSHER_20", "blusher/mu_blush_20.png", FaceMakeup.FACE_MAKEUP_TYPE_BLUSHER, R.drawable.demo_blush_20, R.string.makeup_radio_blusher, false),
    MAKEUP_BLUSHER_21("MAKEUP_BLUSHER_21", "blusher/mu_blush_21.png", FaceMakeup.FACE_MAKEUP_TYPE_BLUSHER, R.drawable.demo_blush_21, R.string.makeup_radio_blusher, true),
    MAKEUP_BLUSHER_22("MAKEUP_BLUSHER_22", "blusher/mu_blush_22.png", FaceMakeup.FACE_MAKEUP_TYPE_BLUSHER, R.drawable.demo_blush_22, R.string.makeup_radio_blusher, false),
    MAKEUP_BLUSHER_23("MAKEUP_BLUSHER_23", "blusher/mu_blush_23.png", FaceMakeup.FACE_MAKEUP_TYPE_BLUSHER, R.drawable.demo_blush_23, R.string.makeup_radio_blusher, false),

    // 眉毛
    MAKEUP_EYEBROW_01("MAKEUP_EYEBROW_01", "eyebrow/mu_eyebrow_01.png", FaceMakeup.FACE_MAKEUP_TYPE_EYEBROW, R.drawable.demo_eyebrow_01, R.string.makeup_radio_eyebrow, false),
    MAKEUP_EYEBROW_10("MAKEUP_EYEBROW_10", "eyebrow/mu_eyebrow_10.png", FaceMakeup.FACE_MAKEUP_TYPE_EYEBROW, R.drawable.demo_eyebrow_10, R.string.makeup_radio_eyebrow, true),
    MAKEUP_EYEBROW_12("MAKEUP_EYEBROW_12", "eyebrow/mu_eyebrow_12.png", FaceMakeup.FACE_MAKEUP_TYPE_EYEBROW, R.drawable.demo_eyebrow_12, R.string.makeup_radio_eyebrow, true),
    MAKEUP_EYEBROW_16("MAKEUP_EYEBROW_16", "eyebrow/mu_eyebrow_16.png", FaceMakeup.FACE_MAKEUP_TYPE_EYEBROW, R.drawable.demo_eyebrow_16, R.string.makeup_radio_eyebrow, false),
    MAKEUP_EYEBROW_17("MAKEUP_EYEBROW_17", "eyebrow/mu_eyebrow_17.png", FaceMakeup.FACE_MAKEUP_TYPE_EYEBROW, R.drawable.demo_eyebrow_17, R.string.makeup_radio_eyebrow, true),
    MAKEUP_EYEBROW_18("MAKEUP_EYEBROW_18", "eyebrow/mu_eyebrow_18.png", FaceMakeup.FACE_MAKEUP_TYPE_EYEBROW, R.drawable.demo_eyebrow_18, R.string.makeup_radio_eyebrow, false),
    MAKEUP_EYEBROW_19("MAKEUP_EYEBROW_19", "eyebrow/mu_eyebrow_19.png", FaceMakeup.FACE_MAKEUP_TYPE_EYEBROW, R.drawable.demo_eyebrow_19, R.string.makeup_radio_eyebrow, false),

    // 眼影
    MAKEUP_EYE_SHADOW_01("MAKEUP_EYESHADOW_01", "eyeshadow/mu_eyeshadow_01.png", FaceMakeup.FACE_MAKEUP_TYPE_EYE_SHADOW, R.drawable.demo_eyeshadow_01, R.string.makeup_radio_eye_shadow, false),
    MAKEUP_EYE_SHADOW_16("MAKEUP_EYESHADOW_16", "eyeshadow/mu_eyeshadow_16.png", FaceMakeup.FACE_MAKEUP_TYPE_EYE_SHADOW, R.drawable.demo_eyeshadow_16, R.string.makeup_radio_eye_shadow, true),
    MAKEUP_EYE_SHADOW_17("MAKEUP_EYESHADOW_17", "eyeshadow/mu_eyeshadow_17.png", FaceMakeup.FACE_MAKEUP_TYPE_EYE_SHADOW, R.drawable.demo_eyeshadow_17, R.string.makeup_radio_eye_shadow, true),
    MAKEUP_EYE_SHADOW_18("MAKEUP_EYESHADOW_18", "eyeshadow/mu_eyeshadow_18.png", FaceMakeup.FACE_MAKEUP_TYPE_EYE_SHADOW, R.drawable.demo_eyeshadow_18, R.string.makeup_radio_eye_shadow, false),
    MAKEUP_EYE_SHADOW_19("MAKEUP_EYESHADOW_19", "eyeshadow/mu_eyeshadow_19.png", FaceMakeup.FACE_MAKEUP_TYPE_EYE_SHADOW, R.drawable.demo_eyeshadow_19, R.string.makeup_radio_eye_shadow, true),
    MAKEUP_EYE_SHADOW_20("MAKEUP_EYESHADOW_20", "eyeshadow/mu_eyeshadow_20.png", FaceMakeup.FACE_MAKEUP_TYPE_EYE_SHADOW, R.drawable.demo_eyeshadow_20, R.string.makeup_radio_eye_shadow, false),
    MAKEUP_EYE_SHADOW_21("MAKEUP_EYESHADOW_21", "eyeshadow/mu_eyeshadow_21.png", FaceMakeup.FACE_MAKEUP_TYPE_EYE_SHADOW, R.drawable.demo_eyeshadow_21, R.string.makeup_radio_eye_shadow, false),

    // 口红
    MAKEUP_LIPSTICK_01("MAKEUP_LIPSTICK_01", "lipstick/mu_lip_01.json", FaceMakeup.FACE_MAKEUP_TYPE_LIPSTICK, R.drawable.demo_lip_01, R.string.makeup_radio_lipstick, false),
    MAKEUP_LIPSTICK_16("MAKEUP_LIPSTICK_16", "lipstick/mu_lip_16.json", FaceMakeup.FACE_MAKEUP_TYPE_LIPSTICK, R.drawable.demo_lip_16, R.string.makeup_radio_lipstick, true),
    MAKEUP_LIPSTICK_17("MAKEUP_LIPSTICK_17", "lipstick/mu_lip_17.json", FaceMakeup.FACE_MAKEUP_TYPE_LIPSTICK, R.drawable.demo_lip_17, R.string.makeup_radio_lipstick, true),
    MAKEUP_LIPSTICK_18("MAKEUP_LIPSTICK_18", "lipstick/mu_lip_18.json", FaceMakeup.FACE_MAKEUP_TYPE_LIPSTICK, R.drawable.demo_lip_18, R.string.makeup_radio_lipstick, false),
    MAKEUP_LIPSTICK_19("MAKEUP_LIPSTICK_19", "lipstick/mu_lip_19.json", FaceMakeup.FACE_MAKEUP_TYPE_LIPSTICK, R.drawable.demo_lip_19, R.string.makeup_radio_lipstick, true),
    MAKEUP_LIPSTICK_20("MAKEUP_LIPSTICK_20", "lipstick/mu_lip_20.json", FaceMakeup.FACE_MAKEUP_TYPE_LIPSTICK, R.drawable.demo_lip_20, R.string.makeup_radio_lipstick, false),
    MAKEUP_LIPSTICK_21("MAKEUP_LIPSTICK_21", "lipstick/mu_lip_21.json", FaceMakeup.FACE_MAKEUP_TYPE_LIPSTICK, R.drawable.demo_lip_21, R.string.makeup_radio_lipstick, false);

    // 每个妆容默认强度 0.7，对应至尊美颜效果
    public static final float DEFAULT_BATCH_MAKEUP_LEVEL = 0.7f;

    private String name;
    private String path;
    private int type;
    private int iconId;
    private int strId;
    /**
     * 妆容组合 整体强度
     */
    public final static SparseArray<Float> MAKEUP_OVERALL_LEVEL = new SparseArray<>();
    /**
     * http://confluence.faceunity.com/pages/viewpage.action?pageId=20332259
     * 妆容和滤镜的组合
     */
    public static final SparseArray<Pair<Filter, Float>> MAKEUP_FILTERS = new SparseArray<>(16);

    /**
     * 根据类型查询美妆
     */
    public static List<MakeupItem> getFaceMakeupByType(int type) {
        FaceMakeupEnum[] values = values();
        List<MakeupItem> makeups = new ArrayList<>(16);
        MakeupItem none = MAKEUP_NONE.faceMakeup();
        none.setType(type);
        makeups.add(none);
        for (FaceMakeupEnum value : values) {
            if (value.showInMakeup && value.type == type) {
                makeups.add(value.faceMakeup());
            }
        }
        return makeups;
    }

    static {
        MAKEUP_OVERALL_LEVEL.put(R.string.makeup_peach_blossom, 0.9f);
        MAKEUP_OVERALL_LEVEL.put(R.string.makeup_grapefruit, 1.0f);
        MAKEUP_OVERALL_LEVEL.put(R.string.makeup_clear, 0.9f);
        MAKEUP_OVERALL_LEVEL.put(R.string.makeup_boyfriend, 1.0f);
        MAKEUP_OVERALL_LEVEL.put(R.string.makeup_red_tea, 1.0f);
        MAKEUP_OVERALL_LEVEL.put(R.string.makeup_winter, 0.9f);
        MAKEUP_OVERALL_LEVEL.put(R.string.makeup_cream, 1.0f);
    }

    // 桃花、西柚、清透、男友, 赤茶妆、冬日妆、奶油妆，
    static {
        MAKEUP_FILTERS.put(R.string.makeup_peach_blossom, Pair.create(Filter.create(Filter.Key.FENNEN_3), 1.0f));
        MAKEUP_FILTERS.put(R.string.makeup_grapefruit, Pair.create(Filter.create(Filter.Key.LENGSEDIAO_4), 0.7f));
        MAKEUP_FILTERS.put(R.string.makeup_clear, Pair.create(Filter.create(Filter.Key.XIAOQINGXIN_1), 0.8f));
        MAKEUP_FILTERS.put(R.string.makeup_boyfriend, Pair.create(Filter.create(Filter.Key.XIAOQINGXIN_3), 0.9f));
        MAKEUP_FILTERS.put(R.string.makeup_red_tea, Pair.create(Filter.create(Filter.Key.XIAOQINGXIN_2), 0.75f));
        MAKEUP_FILTERS.put(R.string.makeup_winter, Pair.create(Filter.create(Filter.Key.NUANSEDIAO_1), 0.8f));
        MAKEUP_FILTERS.put(R.string.makeup_cream, Pair.create(Filter.create(Filter.Key.BAILIANG_1), 0.75f));
    }

    private boolean showInMakeup;

    FaceMakeupEnum(String name, String path, int type, int iconId, int strId, boolean showInMakeup) {
        this.name = name;
        this.path = path;
        this.type = type;
        this.iconId = iconId;
        this.strId = strId;
        this.showInMakeup = showInMakeup;
    }

    /**
     * 美颜模块的美妆组合 资源和顺序为：桃花、西柚、清透、男友
     *
     * @return
     */
    public static List<FaceMakeup> getBeautyFaceMakeup() {
        List<FaceMakeup> faceMakeups = new ArrayList<>();

        FaceMakeup none = new FaceMakeup(null, R.string.makeup_radio_remove, R.drawable.makeup_none_normal);
        faceMakeups.add(none);

        // 桃花
        List<MakeupItem> peachBlossomMakeups = new ArrayList<>(8);
        peachBlossomMakeups.add(MAKEUP_BLUSHER_01.faceMakeup(0.9f));
        peachBlossomMakeups.add(MAKEUP_EYE_SHADOW_01.faceMakeup(0.9f));
        peachBlossomMakeups.add(MAKEUP_EYEBROW_01.faceMakeup(0.5f));
        peachBlossomMakeups.add(MAKEUP_LIPSTICK_01.faceMakeup(0.9f));
        FaceMakeup peachBlossom = new FaceMakeup(peachBlossomMakeups, R.string.makeup_peach_blossom, R.drawable.demo_makeup_peachblossom);
        faceMakeups.add(peachBlossom);

        // 西柚
        List<MakeupItem> grapeMakeups = new ArrayList<>(4);
        grapeMakeups.add(MAKEUP_BLUSHER_23.faceMakeup(1.0f));
        grapeMakeups.add(MAKEUP_EYE_SHADOW_21.faceMakeup(0.75f));
        grapeMakeups.add(MAKEUP_EYEBROW_19.faceMakeup(0.6f));
        grapeMakeups.add(MAKEUP_LIPSTICK_21.faceMakeup(0.8f));
        FaceMakeup grape = new FaceMakeup(grapeMakeups, R.string.makeup_grapefruit, R.drawable.demo_makeup_grapefruit);
        faceMakeups.add(grape);

        // 清透
        List<MakeupItem> clearMakeups = new ArrayList<>(4);
        clearMakeups.add(MAKEUP_BLUSHER_22.faceMakeup(0.9f));
        clearMakeups.add(MAKEUP_EYE_SHADOW_20.faceMakeup(0.65f));
        clearMakeups.add(MAKEUP_EYEBROW_18.faceMakeup(0.45f));
        clearMakeups.add(MAKEUP_LIPSTICK_20.faceMakeup(0.8f));
        FaceMakeup clear = new FaceMakeup(clearMakeups, R.string.makeup_clear, R.drawable.demo_makeup_clear);
        faceMakeups.add(clear);

        // 男友
        List<MakeupItem> boyFriendMakeups = new ArrayList<>(4);
        boyFriendMakeups.add(MAKEUP_BLUSHER_20.faceMakeup(0.8f));
        boyFriendMakeups.add(MAKEUP_EYE_SHADOW_18.faceMakeup(0.9f));
        boyFriendMakeups.add(MAKEUP_EYEBROW_16.faceMakeup(0.65f));
        boyFriendMakeups.add(MAKEUP_LIPSTICK_18.faceMakeup(1.0f));
        FaceMakeup boyFriend = new FaceMakeup(boyFriendMakeups, R.string.makeup_boyfriend, R.drawable.demo_makeup_boyfriend);
        faceMakeups.add(boyFriend);
        return faceMakeups;
    }

    /**
     * 预置的美妆
     *
     * @return
     */
    public static List<FaceMakeup> getDefaultMakeups() {
        List<FaceMakeup> faceMakeups = new ArrayList<>();
        FaceMakeup none = new FaceMakeup(null, R.string.makeup_radio_remove, R.drawable.makeup_none_normal);
        faceMakeups.add(none);

        // 赤茶
        List<MakeupItem> redTeaMakeups = new ArrayList<>(4);
        redTeaMakeups.add(MAKEUP_BLUSHER_18.faceMakeup(1.0f));
        redTeaMakeups.add(MAKEUP_EYE_SHADOW_16.faceMakeup(1.0f));
        redTeaMakeups.add(MAKEUP_EYEBROW_10.faceMakeup(0.6f));
        redTeaMakeups.add(MAKEUP_LIPSTICK_16.faceMakeup(0.9f));
        FaceMakeup redTea = new FaceMakeup(redTeaMakeups, R.string.makeup_red_tea, R.drawable.demo_makeup_red_tea);
        faceMakeups.add(redTea);

        // 冬日
        List<MakeupItem> winterMakeups = new ArrayList<>(4);
        winterMakeups.add(MAKEUP_BLUSHER_19.faceMakeup(0.8f));
        winterMakeups.add(MAKEUP_EYE_SHADOW_17.faceMakeup(0.8f));
        winterMakeups.add(MAKEUP_EYEBROW_12.faceMakeup(0.6f));
        winterMakeups.add(MAKEUP_LIPSTICK_17.faceMakeup(0.9f));
        FaceMakeup winter = new FaceMakeup(winterMakeups, R.string.makeup_winter, R.drawable.demo_makeup_winter);
        faceMakeups.add(winter);

        // 奶油
        List<MakeupItem> creamMakeups = new ArrayList<>(4);
        creamMakeups.add(MAKEUP_BLUSHER_21.faceMakeup(1.0f));
        creamMakeups.add(MAKEUP_EYE_SHADOW_19.faceMakeup(0.95f));
        creamMakeups.add(MAKEUP_EYEBROW_17.faceMakeup(0.5f));
        creamMakeups.add(MAKEUP_LIPSTICK_19.faceMakeup(0.75f));
        FaceMakeup cream = new FaceMakeup(creamMakeups, R.string.makeup_cream, R.drawable.demo_makeup_cream);
        faceMakeups.add(cream);

        return faceMakeups;
    }

    public MakeupItem faceMakeup() {
        return new MakeupItem(name, path, type, strId, iconId);
    }

    public MakeupItem faceMakeup(float level) {
        return new MakeupItem(name, path, type, strId, iconId, level);
    }
}