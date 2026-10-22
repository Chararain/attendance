package attendance;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Simple Attendance by Seat Id.
 *
 * One servlet, three behaviors decided by the studentId parameter:
 *   - studentId == null    : dump the sid2seat map (Attendee List)
 *   - studentId not blank  : record (studentId, seatId) and confirm
 *   - studentId blank      : error message, links back to the pages
 */
@WebServlet("/Attend")
public class Attend extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /** Kept in memory for the lifetime of the web application. */
    private static final Map<String, String> sid2seat = new HashMap<String, String>();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        handle(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        handle(request, response);
    }

    private void handle(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String studentId = request.getParameter("studentId");
        String seatId    = request.getParameter("seatId");

        out.println("Simple Attendance by Seat Id<br><br>");

        if (studentId == null) {
            // ----- Attendee List: dump the map -----
            synchronized (sid2seat) {
                if (sid2seat.isEmpty()) {
                    out.println("No sign-in record yet.<br><br>");
                } else {
                    for (Map.Entry<String, String> e : sid2seat.entrySet()) {
                        out.println(e.getKey() + " : " + e.getValue() + "<br>");
                    }
                }
            }
            out.println("<br><a href=\"index.html\">Attendance Submission</a>");
        } else if (!studentId.trim().isEmpty()) {
            // ----- Record the pair -----
            synchronized (sid2seat) {
                sid2seat.put(studentId.trim(), seatId == null ? "" : seatId.trim());
            }
            out.println(studentId + ":" + seatId + " is recorded:<br><br>");
            out.println("<a href=\"index.html\">Attendance Submission</a> | "
                      + "<a href=\"Attend\">Attendee List</a>");
        } else {
            // ----- Blank student id -----
            out.println("Student Id can not be blank<br><br>");
            out.println("<a href=\"index.html\">Attendance Submission</a> | "
                      + "<a href=\"Attend\">Attendee List</a>");
        }
    }
}
