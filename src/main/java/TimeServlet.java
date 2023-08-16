import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

@WebServlet(value = "/time")
public class TimeServlet extends HttpServlet {

    private TemplateEngine engine;

    @Override
    public void init(ServletConfig config) throws ServletException {
        engine = new TemplateEngine();
        String templates = getClass().getClassLoader().getResource("templates").getPath();

        FileTemplateResolver resolver = new FileTemplateResolver();
        resolver.setPrefix(templates);
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML5");
        resolver.setOrder(engine.getTemplateResolvers().size());
        resolver.setCacheable(false);
        engine.addTemplateResolver(resolver);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        final SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        String format = f.format(new Date());
        if (req.getParameter("timezone") != null) {
            int timezone = Integer.parseInt(req.getParameter("timezone").replace("UTC", "").trim());
            format = changeTime(f, timezone);
            format = format.replace("UTC", req.getParameter("timezone"));
        }
        resp.setContentType("text/html; charset=utf-8");

        Map<String, String[]> parameterMap = req.getParameterMap();

        if (parameterMap.isEmpty()) {
            if (req.getHeader("Cookie") != null) {
                String[] split = req.getHeader("Cookie").split(";");
                for (String pair : split) {
                    String[] splitPairs = pair.split("=");
                    if (splitPairs[0].trim().equals("lastTimezone")) {
                        int time = Integer.parseInt(splitPairs[1].replaceAll("[UTC+-]", ""));
                        format = changeTime(f, time);
                        format = format.replace("UTC", splitPairs[1]);
                        break;
                    }
                }
            }
        } else {
            for (Map.Entry<String, String[]> stringEntry : parameterMap.entrySet()) {
                if (stringEntry.getKey().equals("timezone")) {
                    resp.addCookie(new Cookie("lastTimezone", stringEntry.getValue()[0]));
                    break;
                }
            }
        }

        Context context = new Context(
                req.getLocale(),
                Map.of("queryParams", Map.of("time", format))
        );
        engine.process("index", context, resp.getWriter());
        resp.getWriter().close();
    }

    private static String changeTime(SimpleDateFormat f, int timezone) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.HOUR_OF_DAY, timezone);
        return f.format(calendar.getTime());
    }
}