package cc.ashclaw.fastCloud.api.system.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

@Data
@NoArgsConstructor
public class LoginUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String tenantId;

    private Long userId;

    private Long deptId;

    private String deptCategory;

    private String deptName;

    private String token;

    private String userType;

    private Long loginTime;

    private Long expireTime;

    private String ipaddr;

    private String loginLocation;

    private String browser;

    private String os;

    private Set<String> menuPermission;

    private Set<String> rolePermission;

    private String userName;

    private String nickName;

    private String password;

    private Long roleId;

    private String clientKey;

    private String deviceType;

    public String getLoginId() {
        if (userType == null) {
            throw new IllegalArgumentException("用户类型不能为空");
        }

        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        return userType + ":" + userId;
    }
}
