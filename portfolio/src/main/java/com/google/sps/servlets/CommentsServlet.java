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

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.gson.Gson;
import com.google.sps.data.Comment;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/** Servlet that returns comments content. */
@WebServlet("/comments")
public class CommentsServlet extends HttpServlet {

  private DatastoreService dataStore = DatastoreServiceFactory.getDatastoreService();

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Query query = new Query("comment").addSort("timestamp", SortDirection.DESCENDING);

    PreparedQuery results = dataStore.prepare(query);

    List<Comment> comments = new ArrayList<>();
    for (Entity entity : results.asIterable()) {
      long id = entity.getKey().getId();
      String text = (String) entity.getProperty("comment");
      String username = (String) entity.getProperty("username");
      long timestamp = (long) entity.getProperty("timestamp");

      if (username.isEmpty()) {
        username = "Anonymous User";
      }

      Comment comment = new Comment(id, text, username, timestamp);
      comments.add(comment);
    }

    String json = getCommentsJson(comments);
    response.setContentType("application/json;");
    response.getWriter().println(json);
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String text = getParameter(request, "text-input", "");
    String username = getParameter(request, "username", "");

    if (!text.isEmpty()) {
      long timestamp = System.currentTimeMillis();
      Entity commentEntity = new Entity("comment");

      commentEntity.setProperty("comment", text);
      commentEntity.setProperty("username", username);
      commentEntity.setProperty("timestamp", timestamp);

      dataStore.put(commentEntity);
    }

    response.sendRedirect("/");
  }

  /**
   * Converts the comments array into a JSON string using Gson.
   */
  private String getCommentsJson(List<Comment> comments) {
    Gson gson = new Gson();
    return String.format("{ \"comments\": %s }", gson.toJson(comments));
  }

  /**
   * @return the request parameter, or the default value if the parameter
   *         was not specified by the client
   */
  private String getParameter(HttpServletRequest request, String name, String defaultValue) {
    String value = request.getParameter(name);
    if (value == null) {
      return defaultValue;
    }
    return value;
  }
}
