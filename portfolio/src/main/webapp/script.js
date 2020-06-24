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

/**
 * Loads comments upon page refreshes.
 */
document.addEventListener("DOMContentLoaded", function(){ 
  getCommentsContent(); 
  isLoggedin();
});

/**
 * Adds a random greeting to the page.
 */
function addRandomGreeting() {
  const greetings =
    ['Hello world!', '¡Hola Mundo!', '你好，世界！', 'Bonjour le monde!'];

  // Pick a random greeting.
  const greeting = greetings[Math.floor(Math.random() * greetings.length)];

  // Add it to the page.
  const greetingContainer = document.getElementById('greeting-container');
  greetingContainer.innerText = greeting;
}

/**
 * Fetch data servlet to the page.
 */
function getDataContent() {
  fetch('/data').then(response => response.text()).then(data => {
    document.getElementById('data-servlet').innerText = data;
  });
}

/**
  * Fetch comments servlet to the page.
  */
function getCommentsContent() {
  fetch('/comments').then(response => response.json()).then((commentsJson) => {
    const commentsListElement = document.getElementById('comments-servlet');
    commentsListElement.innerHTML = '';
    for (i in commentsJson.comments) {
      commentsListElement.appendChild(
        createListElement(commentsJson.comments[i].comment, commentsJson.comments[i].username));
      const imageUrl = commentsJson.comments[i].imageUrl
      if (imageUrl != null) {
        commentsListElement.innerHTML += `<img src="${imageUrl}">`;
        addImageLabels(commentsJson.comments[i].imageLabels, commentsListElement)
      }
    }
  });
}

/*
 * Creates an a paragraph of image text labels.
 */
function addImageLabels(imageLabels, commentsListElement) {
  if (imageLabels != null) {
    imageLabelsText = "<p>This is a/an: "
    for (i in imageLabels) {
      imageLabelsText += imageLabels[i] + ", "
    }
    imageLabelsText = imageLabelsText.substring(0, imageLabelsText.length - 2) + " image </p>"
    commentsListElement.innerHTML += imageLabelsText
  }
}

/*
 * Creates an <li> element containing text. 
 */
function createListElement(text, username) {
  const liElement = document.createElement('li');
  liElement.innerText = text + " - by " + username;
  return liElement;
}

/*
 * Check if user is logged in.
 */
function isLoggedin() {
  fetch('/login').then(response => response.json()).then(userJson => {
    const commentsSection = document.getElementById('comments-section')
    if (userJson.userEmail) {
      commentsSection.innerHTML = commentsSectionLoggedIn(userJson.userEmail, userJson.url, userJson.uploadUrl)
    } else {
      commentsSection.innerHTML = commentsSectionLoggedOut(userJson.url)
    }
  });
}

function commentsSectionLoggedIn(userEmail, logoutUrl, uploadUrl) {
  return `
  <form method="POST" enctype="multipart/form-data" action="${uploadUrl}" method="POST">
    <p>Hello ${userEmail}! Enter any comments (multiple comments are separated by enter):</p>
    <textarea name="text-input" placeholder="Enter anything you like~" rows="5" cols="50"></textarea>
    <br/>
    <label for="imageFile">Select a image file:</label>
    <input type="file" id="imageFile" name="imageFile"><br><br>
    <br/>
    <input type="submit"/>
  </form>
  <p>Alternatively, you can logout <a href="${logoutUrl}">here</a>.</p>`;
}

function commentsSectionLoggedOut(loginUrl) {
  return `<p>Hello, login <a href="${loginUrl}">here</a> to leave a comment.`
}
