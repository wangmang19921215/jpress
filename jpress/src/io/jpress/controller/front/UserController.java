/**
 * Copyright (c) 2015-2016, Michael Yang 杨福海 (fuhai999@gmail.com).
 *
 * Licensed under the GNU Lesser General Public License (LGPL) ,Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.jpress.controller.front;

import io.jpress.Consts;
import io.jpress.core.annotation.UrlMapping;
import io.jpress.interceptor.UCodeInterceptor;
import io.jpress.interceptor.UserInterceptor;
import io.jpress.model.User;
import io.jpress.plugin.message.MessageKit;
import io.jpress.plugin.message.listener.Actions;
import io.jpress.utils.CookieUtils;
import io.jpress.utils.HashUtils;
import io.jpress.utils.StringUtils;

import java.util.Date;

import com.jfinal.aop.Before;
import com.jfinal.aop.Clear;

@UrlMapping(url = Consts.USER_BASE_URL)
@Before(UserInterceptor.class)
public class UserController extends BaseFrontController {

	@Clear
	public void index() {
		String action = getPara();
		if (StringUtils.isNotBlank(action)) {
			keepPara();
			render(String.format("user_%s.html", action));
		} else {
			renderError(404);
		}
	}

	// 固定登陆的url
	@Clear
	public void login() {
		keepPara();
		render("user_login.html");
	}

	@Clear
	public void doLogin() {
		long errorTimes = CookieUtils.getLong(this, "_login_errors", 0);
		if (errorTimes >= 3) {
			if (!validateCaptcha("_login_captcha")) { // 验证码没验证成功！
				if (isAjaxRequest()) {
					renderAjaxResultForError("没有该用户");
				} else {
					redirect(Consts.LOGIN_BASE_URL);
				}
				return;
			}
		}

		String username = getPara("username");
		String password = getPara("password");
		String from = getPara("from");

		User user = User.DAO.findUserByUsername(username);
		if (null == user) {
			if (isAjaxRequest()) {
				renderAjaxResultForError("没有该用户");
			} else {
				redirect(Consts.LOGIN_BASE_URL);
			}
			CookieUtils.put(this, "_login_errors", errorTimes + 1);
			return;
		}

		if (HashUtils.verlifyUser(user, password)) {
			MessageKit.sendMessage(Actions.USER_LOGINED, user);
			CookieUtils.put(this, Consts.COOKIE_LOGINED_USER, user.getId());
			if (this.isAjaxRequest()) {
				renderAjaxResultForSuccess("登陆成功");
			} else {
				if (StringUtils.isNotEmpty(from)) {
					redirect(from);
				} else {
					redirect(Consts.USER_CENTER_BASE_URL);
				}
			}
		} else {
			if (isAjaxRequest()) {
				renderAjaxResultForError("密码错误");
			} else {
				redirect(Consts.LOGIN_BASE_URL);
			}
			CookieUtils.put(this, "_login_errors", errorTimes + 1);
		}
	}

	@Before(UCodeInterceptor.class)
	public void logout() {
		CookieUtils.remove(this, Consts.COOKIE_LOGINED_USER);
		redirect("/");
	}

	@Clear
	public void doRegister() {
		if (!validateCaptcha("_register_captcha")) { // 验证码没验证成功！
			renderAjaxResult("not validate captcha", Consts.ERROR_CODE_NOT_VALIDATE_CAPTHCHE);
			return;
		}

		String username = getPara("username");
		String email = getPara("email");
		String phone = getPara("phone");
		String password = getPara("password");

		if (!StringUtils.isNotBlank(username)) {
			renderAjaxResult("username is empty!", Consts.ERROR_CODE_USERNAME_EMPTY);
			return;
		}

		if (!StringUtils.isNotBlank(email)) {
			renderAjaxResult("email is empty!", Consts.ERROR_CODE_EMAIL_EMPTY);
			return;
		}

		if (!StringUtils.isNotBlank(password)) {
			renderAjaxResult("password is empty!", Consts.ERROR_CODE_PASSWORD_EMPTY);
			return;
		}

		if (User.DAO.findUserByUsername(username) != null) {
			renderAjaxResult("username has exist!", Consts.ERROR_CODE_USERNAME_EXIST);
			return;
		}

		if (User.DAO.findUserByEmail(email) != null) {
			renderAjaxResult("email has exist!", Consts.ERROR_CODE_EMAIL_EXIST);
			return;
		}

		if (null != phone && User.DAO.findUserByPhone(phone) != null) {
			renderAjaxResult("phone has exist!", Consts.ERROR_CODE_PHONE_EXIST);
			return;
		}

		User user = new User();
		user.setUsername(username);
		user.setEmail(email);
		user.setPhone(phone);

		String salt = HashUtils.salt();
		password = HashUtils.md5WithSalt(password, salt);
		user.setPassword(password);
		user.setSalt(salt);
		user.setCreateSource("register");
		user.setCreated(new Date());
		user.save();

		CookieUtils.put(this, Consts.COOKIE_LOGINED_USER, user.getId());

		MessageKit.sendMessage(Actions.USER_CREATED, user);

		if (isAjaxRequest()) {
			renderAjaxResultForSuccess();
		} else {
			redirect("/user/center");
		}
	}

	public void center() {
		keepPara();
		String action = getPara(0, "index");
		render(String.format("ucenter_%s.html", action));
	}

	public void update() {

	}
}
