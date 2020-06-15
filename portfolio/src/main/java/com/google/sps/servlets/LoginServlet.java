// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps.servlets;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();

    UserService userService = UserServiceFactory.getUserService();

    if (userService.isUserLoggedIn()) {
      String userEmail = userService.getCurrentUser().getEmail();
      String urlToRedirectToAfterUserLogsOut = "/";
      String logoutUrl = userService.createLogoutURL(urlToRedirectToAfterUserLogsOut);

      out.println("<form action=\"/comments\" method=\"POST\">");
      out.println("<p>Hello " + userEmail + "! Enter any comments (multiple comments are separated by enter):</p>");
      out.println("<textarea name=\"text-input\" placeholder=\"Enter anything you like~\" rows=\"5\" cols=\"50\"></textarea>");
      out.println("<br/><br/>");
      out.println("<label for=\"username\">By user:</label>");
      out.println("<input name=\"username\" id=\"username\" type=\"text\" value=\"" + userEmail + "\" />");
      out.println("<br/><br/>");
      out.println("<input type=\"submit\"/></form>");
      out.println("<p>Alternatively, you can logout <a href=\"" + logoutUrl + "\">here</a>.</p>");
    } else {
      String urlToRedirectToAfterUserLogsIn = "/";
      String loginUrl = userService.createLoginURL(urlToRedirectToAfterUserLogsIn);

      out.println("<p>Hello, login <a href=\"" + loginUrl + "\">here</a> to leave a comment.");
    }
  }
}
