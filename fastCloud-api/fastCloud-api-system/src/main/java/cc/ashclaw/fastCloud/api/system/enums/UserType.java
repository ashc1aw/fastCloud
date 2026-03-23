package cc.ashclaw.fastCloud.api.system.enums;

import cc.ashclaw.common4j.core.util.StringUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserType {

    SYS_USER("sys_user"),
    APP_USER("app_user");

    private final String userType;

    public static UserType getUserType(String str) {
        for (UserType value : values()) {
            if (StringUtil.contains(str, value.getUserType())) {
                return value;
            }
        }
        throw new RuntimeException("UserType not found by" + str);
    }


}
