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

import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet that returns comments content. */
@WebServlet("/comments")
public class CommentsServlet extends HttpServlet {

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    ArrayList<String> comments = new ArrayList<>();
    comments.add("Nice looking website there Jacob!");
    comments.add("You need to work on your CSS Jacob.");
    comments.add("Making good progress~ :)");

    String json = convertArrayToJson(comments);

    response.setContentType("application/json;");
    response.getWriter().println(json);
  }

  /**
   * Converts the array list into a JSON string using Gson.
   */
  private String convertArrayToJson(ArrayList<String> comments) {
    Gson gson = new Gson();
    String json = "{";
    json += "\"comments\": ";
    json += gson.toJson(comments);
    json += "}";
    return json;
  }
}
