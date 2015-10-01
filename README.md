# Scala.js spreadsheet

![spreadsheeeeeit](http://24.media.tumblr.com/tumblr_lrhm9lVLLi1qfq1lso1_500.jpg)

A very simple spreadsheet built using scala.js. Basically it follows this [article](http://semantic-domain.blogspot.nl/2015/07/how-to-implement-spreadsheet.html).

It uses [scala-parser-combinators](https://github.com/scala/scala-parser-combinators) to parse user input.

To run, first compile:

```bash
> sbt fastOptJS
```

and open the file `index.html`