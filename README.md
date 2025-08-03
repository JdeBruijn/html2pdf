
# Html2Pdf

### Description
I couldn't find a Html to PDF converter that worked with my style of HTML, so I wrote one.
It's rarely necessary to have to generate a PDF directly from HTML but I've encountered this
requirement a couple of times and figured is was worth making a converter that works for me.


## Features

### In-built CSS Inliner
That's right! This converter allows you to specify a CSS file to go along with 
your HTML file. See **Useage** section on how to specify the css file.

### Custom Fonts Support
You can use whatever Font you like in your HTML as long as you provide the .ttf file in the 
**fonts** folder and update *fonts.json*.
There are more instructions in *fonts.json* on how to add your font.

Check out the sample HTML, CSS and generated PDF files in the **sample** folder for an example
of what this program can do.

### Html to Image
Html2Pdf can also convert Html directly to PNG!
All the same features are supported in both conversion to PDF and PNG.


## Installation
Clone the repo or download the .zip file and extract it.

## Useage (stand alone)

### Linux
For linux users there's a script to make things easy:
```
	./run.sh path/to/yourHTML.html [path/to/yourCSS.css]
```
Output will be written to path/to/yourHTML.pdf

Run sample:
```
	./run.sh sample/sample.html /sample/sample.css
```
Output writtent to sample/sample.pdf

### Other systems
Build:
```
	javac -cp "lib/openpdf-2.0.3.jar" -d classes/ src/*.java
```
Run:
```
	java -cp "lib/openpdf-2.0.3.jar:./classes/"  HtmlToPdfConverter path/to/yourHTML.html [path/to/yourCSS.css]
```
Output will be written to path/to/yourHTML.pdf

Run sample:
```
	java -cp "lib/openpdf-2.0.3.jar:./classes/" sample/sample.html /sample/sample.css
```
Output writtent to sample/sample.pdf


## Requirements

### Java
Project written in Java-17

### OpenPdf
The actual PDF generation uses OpenPDF. A version of the openpdf jar is included in the lib
folder though so there are no external dependencies.
If you're wanting to use this code as part of a bigger project you might have to remove that
and update your project dependencies instead.

### Linux
Not a hard requirement but the super convenient 'run.sh' script won't work unless you're using
linux (or maybe MacOS? I haven't tried).


## Limitations

### Tables
Only supports 'table-layout: fixed;'
Column width is set by the first row of the table.
'Relative' widthing NOT supported. ie: percentage widths must add up to 100% to fit correctly.
If widths of first row cells not set then the cells will shrink to fit the contents and all cells
 in the column will have the same width.
Default borders:
 table: 'top' and 'left' set to '1px solid black'
 row: 'bottom' set to '1px solid black'
 cell: 'left' set to '1px solid black'
Can be overriden by specifically setting borders on table, row, cells to something else.


### Default tag styling
The second biggest limitation is probably that I haven't baked in the default styling of most html
tags (span, h1, input, etc...).
It is possible to work around this using the CSS Inliner. Just add the default css for your tag(s)
to your .css file and then it will be applied.

### Mixing of flows
Mixing of block, float, inline-block elements might not create exactly the same layout as a 
browser would. It's generally not recommended to mix flows in html too much anyway though since
the results tend to be a bit unpredictable.

