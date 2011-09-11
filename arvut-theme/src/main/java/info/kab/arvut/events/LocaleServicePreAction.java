package info.kab.arvut.events;

import com.liferay.portal.kernel.events.Action;
import com.liferay.portal.kernel.events.ActionException;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.uuid.PortalUUID;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.Organization;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.OrganizationLocalServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.Portal;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.expando.service.ExpandoValueLocalServiceUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Set the user's Locale to the one defined for the Organization
 */
public class LocaleServicePreAction extends Action {

	@Override
	public void run(HttpServletRequest request, HttpServletResponse response)
			throws ActionException {

		HttpSession session = request.getSession();

		Locale locale = null;
		try {
			long scopeGroupId = PortalUtil.getScopeGroupId(request);

			Locale currentLocale = (Locale) session.getAttribute(LOCALE_KEY);
			locale = getLocaleFromGroup(scopeGroupId, currentLocale);

		} catch (Exception e) {
			_log.error("Error reading Group for Locale", e);
		}

		if (locale != null) {
			session.setAttribute(LOCALE_KEY, locale);
			LanguageUtil.updateCookie(request, response, locale);

			ThemeDisplay themeDisplay =
				 (ThemeDisplay)request.getAttribute(WebKeys.THEME_DISPLAY);

			try {
				String url = PortalUtil.getLayoutFullURL(themeDisplay);
				response.sendRedirect(url);
			} catch (Exception e) { _log.error(e); }
		}
	}

	private Locale getLocaleFromGroup(long groupId, Locale currentLocale)
			throws Exception {
		Group group = GroupLocalServiceUtil.getGroup(groupId);
		long orgId = group.getOrganizationId();


		String[] languageIds;
		try {
			Organization org =
					OrganizationLocalServiceUtil.getOrganization(orgId);

			languageIds =
				(String[]) org.getExpandoBridge().getAttribute(LOCALES_ATT);
		} catch (Exception ignore) {
			languageIds =
				(String[]) group.getExpandoBridge().getAttribute(LOCALES_ATT);
		}

		if (languageIds == null || languageIds.length == 0) {
			// Can't do anything
			return null;
		}

		String currentLanguageId = LocaleUtil.toLanguageId(currentLocale);
		for (String languageId : languageIds) {
			if (currentLanguageId.equals(languageId)) {
				// No need to update the locale
				return null;
			}
		}

		// If we got this far, then return the first available locale for this group
		return LocaleUtil.fromLanguageId(languageIds[0]);
	}

	private static Log _log =
			LogFactoryUtil.getLog(LocaleServicePreAction.class);

	/** Copied out of org.apache.struts.Global - will break if changed */
	private static final String LOCALE_KEY = "org.apache.struts.action.LOCALE";

	public static final String LOCALES_ATT = "Locale";

}
