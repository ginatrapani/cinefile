# Cinefile

A simple application that browses popular movies by presenting a grid of movie posters. All movie information comes from the (The Movie Database (TMDb))[https://www.themoviedb.org].

## Not a production app!

I am working on this app as an exercise in Android development. It is ugly and incomplete, and some features might not work correctly.

## Features

* View a tappable grid of movie posters from the TMDb API which is also tablet optimized
* Tap on a poster to see movie details: title, ratings, reviews, and links to trailers
* Use Android intent to launch YouTube to watch trailers
* Mark movies as a favorite, or unmark as a favorite
* Sort movies by most popular, highest rated, or just see your favorites

## Requires an API key

To build and run this app, you'll need your own TMDb API key. Get one from https://www.themoviedb.org and then add it to the res/values/themoviedb.xml file.

## Screenshots

# Phone view

![Phone portrait view poster grid](https://github.com/ginatrapani/cinefile/blob/master/screenshots/cinefile-phone-portrait-grid.png)

![Phone portrait view movie details](https://github.com/ginatrapani/cinefile/blob/master/screenshots/cinefile-phone-portrait-details.png)

# Tablet view

![Tablet landscape](https://github.com/ginatrapani/cinefile/blob/master/screenshots/cinefile-tablet-landscape-grid-details.png)
