# Supergit

Git implemented in Clojure. Started this project to try & understand how git does the magic underneath.

## Installation

Clone the repo & start a repl session with leiningen in project root.
 
 `lein repl`
 
It's still a work in progress.

## Usage

The commands are not available through a terminal, so require a little extra work.

Copy the path of your working copy

## Examples

In repl :

`(init "path/to/working_copy")` this will initialize 
###### .clogit
directory in your project root.

`(status "path/to/working_copy")`

`(add "path/to/working_copy" "src/file")`

`(commit "path/to/working_copy" "commit msg")`

`(checkout "path/to/working_copy" "-b" "newbranch")`
to create a new branch & switch on it.

`(checkout "path/to/working_copy" "branch_name")`
to switch branches.

`(branch "path/to/working_copy" "branch_name")`
to create a new branch.

`(reset "path/to/working_copy")`

That's all the supported actions until now.
All files inside .clogit are saved as plain text, makes it easy to see what changed inside.

## License

Copyright © 2017 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
