package cn.cerc.jmis.core;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import cn.cerc.jbean.client.LocalService;
import cn.cerc.jbean.core.AppConfig;
import cn.cerc.jbean.core.Application;
import cn.cerc.jbean.form.IForm;
import cn.cerc.jdb.core.IHandle;
import cn.cerc.jdb.core.Record;

public class AppSecurity {
	private static final Logger log = Logger.getLogger(AppSecurity.class);

	private HttpServletRequest req;
	private HttpServletResponse resp;
	private IHandle handle;

	public AppSecurity(HttpServletRequest req, HttpServletResponse resp, IHandle sess) {
		this.req = req;
		this.resp = resp;
		this.handle = sess;
	}

	public boolean execute(IForm form, String token) throws IOException, ServletException {
		AppConfig conf = Application.getConfig();
		String jsp_file_login = conf.getJspLoginFile();
		try {
			if (req.getParameter("login_usr") != null) {
				String userCode = req.getParameter("login_usr");
				String password = req.getParameter("login_pwd");
				log.debug(String.format("校验用户帐号(%s)与密码", userCode));
				req.setAttribute("needVerify", "false");
				if (checkLogin(form, userCode, password))
					return true;
				// try (MemoryBuffer buff = new
				// MemoryBuffer(BufferType.getSessionBase, sess.getID()))
				// {
				// buff.clear();
				// }
				req.setAttribute("homePage", "TFrmWelcome");
				req.getServletContext().getRequestDispatcher(jsp_file_login).forward(req, resp);
				return false;
			}

			log.debug(String.format("根据 token(%s) 创建 Session", token));
			IHandle sess = (IHandle) handle.getProperty(null);
			if (sess.init(token))
				return true;

			if (form.logon())
				return true;
		} catch (Exception e) {
			req.setAttribute("loginMsg", e.getMessage());
		}
		req.setAttribute("needVerify", "false");
		req.setAttribute("homePage", conf.getFormWelcome());
		req.getServletContext().getRequestDispatcher(jsp_file_login).forward(req, resp);
		return false;
	}

	public boolean checkLogin(IForm form, String userCode, String password) {
		// 进行设备首次登记
		String deviceId = form.getClient().getId();
		req.setAttribute("userCode", userCode);
		req.setAttribute("password", password);
		req.setAttribute("needVerify", "false");
		// 如长度大于10表示用手机号码登入
		if (userCode.length() > 10) {
			String oldCode = userCode;
			userCode = getAccountFromTel(form.getHandle(), oldCode);
			log.debug(String.format("将手机号 %s 转化成帐号 %s", oldCode, userCode));
		}
		boolean result = false;

		log.debug(String.format("进行用户帐号(%s)与密码认证", userCode));
		// 进行用户名、密码认证
		LocalService app = new LocalService(form.getHandle());
		app.setService("SvrUserLogin.check");
		if (app.exec("Account_", userCode, "Password_", password, "MachineID_", deviceId)) {
			String sid = app.getDataOut().getHead().getString("SessionID_");
			if (sid != null && !sid.equals("")) {
				log.debug(String.format("认证成功，取得sid(%s)", sid));
				req.setAttribute(RequestData.appSession_Key, sid);
				req.getSession().setAttribute(RequestData.appSession_Key, sid);
				result = true;
			}
		} else {
			log.debug(String.format("用户帐号(%s)与密码认证失败", userCode));
			req.setAttribute("loginMsg", app.getMessage());
		}
		return result;
	}

	/**
	 * 根据电话号码返回用户帐号，用于普及版登入
	 * 
	 * @param tel
	 * @return
	 */
	private String getAccountFromTel(IHandle handle, String tel) {
		LocalService app = new LocalService(handle);
		app.setService("SvrUserLogin.getUserCodeByMobile");
		app.getDataIn().getHead().setField("UserCode_", tel);
		if (!app.exec()) {
			Record headOut = app.getDataOut().getHead();
			throw new RuntimeException(headOut.getString("Msg_"));
		} else
			return app.getDataOut().getHead().getString("UserCode_");
	}

}
