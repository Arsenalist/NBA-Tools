import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.*;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.*;

import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;
import java.util.*;

public class Main extends HttpServlet {
    public static Configuration cfg = new Configuration();
static {


    // Where do we load the templates from:
    cfg.setClassForTemplateLoading(NBA.class, "templates");
    // Some other recommended settings:
    cfg.setIncompatibleImprovements(new Version(2, 3, 20));
    cfg.setDefaultEncoding("UTF-8");
    cfg.setLocale(Locale.US);
    cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
}
    static String css = "table.common,table.preview{width:100%;background:#fff;color:#222}.common,.common td,.preview,.preview td{vertical-align:top;font-size:.8em;font-family:inherit;padding:5px}.common{text-align:center}.common .game-description{margin:5px;font-size:1.8em;padding:0}.common ul li{display:inline-block;background:#fafafa;margin:6px}.common p.tv,.common p.venue,p.odds{font-weight:700;font-size:1.1em}.preview .logo,.preview table .logo{text-align:center}.common ul li,.preview ul li{padding:3px;line-height:1em}.common ul,.preview ul{list-style-type:none;margin:0;padding:0}.preview h4{text-align:center;background:#eee;padding:5px}.preview .logo{text-align:center}";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            if (req.getRequestURI().endsWith("/")) {
                resp.sendRedirect("/preview");
            } else if (req.getRequestURI().endsWith("/preview")) {
                ResourceBundle bundle = ResourceBundle.getBundle("bref_to_thescore");
                Enumeration<String> keys = bundle.getKeys();
                ArrayList<String> list = Collections.list(keys);
                Collections.sort(list);
                Map<String, Object> data = new HashMap<String, Object>();
                data.put("teams", list);
                Template template = Main.cfg.getTemplate("preview-form.html");
                StringWriter writer = new StringWriter();
                template.process(data, writer);
                resp.getWriter().print(writer.getBuffer().toString());
            } else if (req.getRequestURI().endsWith("/result")) {
                String team = req.getParameter("team");
                boolean responsive = req.getParameter("responsive") != null;
                NBA nba = new NBA();
                String html = nba.preview(team, responsive);
                Template template = Main.cfg.getTemplate("result.html");
                StringWriter writer = new StringWriter();
                Map<String, String> data = new HashMap<String, String>();
                data.put("css", css);
                data.put("html", html);
                template.process(data, writer);
                resp.getWriter().print(writer.getBuffer().toString());
            } else {
                showHome(req, resp);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void showHome(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.getWriter().print("Hello from Java!");
    }

    private void showDatabase(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Connection connection = null;
        try {
            connection = getConnection();

            Statement stmt = connection.createStatement();
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ticks (tick timestamp)");
            stmt.executeUpdate("INSERT INTO ticks VALUES (now())");
            ResultSet rs = stmt.executeQuery("SELECT tick FROM ticks");

            String out = "Hello!\n";
            while (rs.next()) {
                out += "Read from DB: " + rs.getTimestamp("tick") + "\n";
            }

            resp.getWriter().print(out);
        } catch (Exception e) {
            resp.getWriter().print("There was an error: " + e.getMessage());
        } finally {
            if (connection != null) try {
                connection.close();
            } catch (SQLException e) {
            }
        }
    }

    private Connection getConnection() throws URISyntaxException, SQLException {
        URI dbUri = new URI(System.getenv("DATABASE_URL"));

        String username = dbUri.getUserInfo().split(":")[0];
        String password = dbUri.getUserInfo().split(":")[1];
        int port = dbUri.getPort();

        String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ":" + port + dbUri.getPath();

        return DriverManager.getConnection(dbUrl, username, password);
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server(Integer.valueOf(System.getenv("PORT")));
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        context.addServlet(new ServletHolder(new Main()), "/*");
        server.start();
        server.join();
    }
}
