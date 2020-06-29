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

import com.google.appengine.api.blobstore.BlobInfo;
import com.google.appengine.api.blobstore.BlobInfoFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.ServingUrlOptions;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import com.google.sps.data.Comment;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Servlet that returns comments content. */
@WebServlet("/comments")
public class CommentsServlet extends HttpServlet {

  private DatastoreService dataStore = DatastoreServiceFactory.getDatastoreService();
  private Feature feature = Feature.newBuilder().setType(Feature.Type.LABEL_DETECTION).build();
  private ImageAnnotatorClient client;
  private double THRESHOLD_ACCURACY = 0.80;

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Query query = new Query("comment").addSort("timestamp", SortDirection.DESCENDING);
    PreparedQuery results = dataStore.prepare(query);

    List<Comment> comments = new ArrayList<>();
    for (Entity entity : results.asIterable()) {
      long id = entity.getKey().getId();
      String text = (String) entity.getProperty("comment");
      String username = (String) entity.getProperty("username");
      String imageUrl = (String) entity.getProperty("imageUrl");
      List<String> imageLabels = (ArrayList<String>)entity.getProperty("imageLabels");
      long timestamp = (long) entity.getProperty("timestamp");

      if (username.isEmpty()) {
        username = "Anonymous User";
      }

      Comment comment = new Comment(id, text, username, imageUrl, imageLabels, timestamp);
      comments.add(comment);
    }

    String json = getCommentsJson(comments);
    response.setContentType("application/json;");
    response.getWriter().println(json);
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

    UserService userService = UserServiceFactory.getUserService();

    String text = getParameter(request, "text-input", "");
    String email = userService.getCurrentUser().getEmail();
    // assumes that all emails will have @ key in the string
    String username = email.split("@")[0];
    String imageUrl = getUploadedFileUrl(request, "imageFile");

    // Get the BlobKey that points to the image uploaded by the user.
    BlobKey blobKey = getBlobKey(request, "imageFile");
    // Get the labels of the image that the user uploaded.
    ByteString blobByteString = getBlobByteString(blobKey);
    List<String> imageLabels = getImageLabels(blobByteString);

    if (!text.isEmpty()) {
      long timestamp = System.currentTimeMillis();
      Entity commentEntity = new Entity("comment");

      commentEntity.setProperty("comment", text);
      commentEntity.setProperty("email", email);
      commentEntity.setProperty("username", username);
      commentEntity.setProperty("imageUrl", imageUrl);
      commentEntity.setProperty("imageLabels", imageLabels);
      commentEntity.setProperty("timestamp", timestamp);

      dataStore.put(commentEntity);
    }

    response.sendRedirect("/");
  }

  /**
   * Converts the comments array into a JSON string using Gson.
   */
  private String getCommentsJson(List<Comment> comments) {
    return String.format("{ \"comments\": %s }", new Gson().toJson(comments));
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

  /** 
   * @return a URL that points to the uploaded file, 
   *         or null if the user didn't upload a file. 
   */
  private String getUploadedFileUrl(HttpServletRequest request, String formInputElementName) {
    BlobKey blobKey = getBlobKey(request, formInputElementName);

    // Use ImagesService to get a URL that points to the uploaded file.
    ImagesService imagesService = ImagesServiceFactory.getImagesService();
    ServingUrlOptions options = ServingUrlOptions.Builder.withBlobKey(blobKey);
    String url = imagesService.getServingUrl(options);

    // GCS's localhost preview is not actually on localhost,
    // so make the URL relative to the current domain.
    if(url.startsWith("http://localhost:8080/")){
      url = url.replace("http://localhost:8080/", "/");
    }
    return url;
  }

  /**
   * Returns the BlobKey that points to the file uploaded by the user, or null if the user didn't
   * upload a file.
   */
  private BlobKey getBlobKey(HttpServletRequest request, String formInputElementName) {
    BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
    Map<String, List<BlobKey>> blobs = blobstoreService.getUploads(request);
    List<BlobKey> blobKeys = blobs.get("imageFile");

    // User submitted form without selecting a file, so we can't get a BlobKey. (dev server)
    if (blobKeys == null || blobKeys.isEmpty()) {
      return null;
    }

    // Our form only contains a single file input, so get the first index.
    BlobKey blobKey = blobKeys.get(0);

    // User submitted form without selecting a file, so the BlobKey is empty. (live server)
    BlobInfo blobInfo = new BlobInfoFactory().loadBlobInfo(blobKey);
    if (blobInfo.getSize() == 0) {
      blobstoreService.delete(blobKey);
      return null;
    }

    return blobKey;
  }

  /**
   * Blobstore stores files as binary data. This function retrieves the binary data stored at the
   * BlobKey parameter.
   */
  private ByteString getBlobByteString(BlobKey blobKey) throws IOException {
    BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
    ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();

    int fetchSize = BlobstoreService.MAX_BLOB_FETCH_SIZE;
    long currentByteIndex = 0;
    while (true) {
      // end index is inclusive, so we have to subtract 1 to get fetchSize bytes
      long endIndex = currentByteIndex + fetchSize - 1;
      byte[] b = blobstoreService.fetchData(blobKey, currentByteIndex, endIndex);
      outputBytes.write(b);

      // if we read fewer bytes than we requested, then we reached the end
      if (b.length < fetchSize) {
        break;
      }
      currentByteIndex += fetchSize;
    }
    ByteArrayInputStream inputStream = new ByteArrayInputStream(outputBytes.toByteArray());
    // more efficient way of returning object
    return ByteString.readFrom(inputStream);
  }

  /**
   * Uses the Google Cloud Vision API to generate a list of labels that apply to the image
   * represented by the binary data stored in imgByteString.
   */
  private List<String> getImageLabels(ByteString imgByteString) throws IOException {
    Image image = Image.newBuilder().setContent(imgByteString).build();

    AnnotateImageRequest request =
        AnnotateImageRequest.newBuilder().addFeatures(feature).setImage(image).build();
    List<AnnotateImageRequest> requests = new ArrayList<>();
    requests.add(request);

    // need to initialize within the method to throw IOException
    client = ImageAnnotatorClient.create();
    BatchAnnotateImagesResponse batchResponse = client.batchAnnotateImages(requests);
    client.close();
    List<AnnotateImageResponse> imageResponses = batchResponse.getResponsesList();
    AnnotateImageResponse imageResponse = imageResponses.get(0);

    if (imageResponse.hasError()) {
      System.err.println("Error getting image labels: " + imageResponse.getError().getMessage());
      return new ArrayList<String>();
    }
    
    return imageResponse.getLabelAnnotationsList()
            .stream()
            .filter(label -> label.getScore() > THRESHOLD_ACCURACY)
            .map(label -> label.getDescription())
            .collect(Collectors.toList());
  }
}