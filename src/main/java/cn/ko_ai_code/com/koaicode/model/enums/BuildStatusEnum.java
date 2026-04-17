package cn.ko_ai_code.com.koaicode.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * Vue项目构建状态枚举
 */
@Getter
public enum BuildStatusEnum {

    PENDING("待构建", "pending"),
    INSTALLING("正在安装依赖", "installing"),
    BUILDING("正在构建项目", "building"),
    SUCCESS("构建成功", "success"),
    FAILED("构建失败", "failed");

    private final String text;
    private final String value;

    BuildStatusEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的value
     * @return 枚举值
     */
    public static BuildStatusEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (BuildStatusEnum anEnum : BuildStatusEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
