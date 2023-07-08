package cn.bugstack.dev.tech.dubbo.api;

import cn.bugstack.dev.tech.dubbo.api.dto.UserReqDTO;
import cn.bugstack.dev.tech.dubbo.api.dto.UserResDTO;
import cn.bugstack.dev.tech.dubbo.api.types.Response;

/**
 * 查询用户信息
 *
 * @author fuzhengwei
 */
public interface IUserService {

    Response<UserResDTO> queryUserInfo(UserReqDTO reqDTO);

}
