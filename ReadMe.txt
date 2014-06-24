############################################################
# Install each application or libraries, and environment this project.

You have to download and install applications below.

 - If your environment is windows, you must be install Mingw + msys environment.
 - JDK (now, using JDK version 8.)
 - Apache ant (now, using version1.9.4)
 - If your environment is windows, you must be install ctags. (now using ctags58j2bin)
 - h2 database (probably, you can use latest version)
 - subversion (now, using version 1.6.16)
 - Any text editor or IDE. (I'm using GVIM for windows, it is very powerfull and I love it!)

When you installed above applications after, you have to set PATH environment variable.
$HOME/.profile for msys shell is below.

---
alias ls='ls --color=auto --show-control-chars'
alias vi='gvim' # for me!

# Applications and 3rd party libraries.
export APPDIR=~/apps

# for java.
export JAVA_HOME=$APPDIR/jdk1.6.0_24
export JAVA_BIN=$JAVA_HOME/bin

# for gvim for windows commands. (for me!)
export GVIM_BIN=$APPDIR/vim73-kaoriya-win32

# for apache ant commands.
export ANT_BIN=$APPDIR/apache-ant-1.8.2/bin

# for H2 database commands.
export H2_BIN=$APPDIR/h2/bin

# for Apache tomcat commands.
export TOMCAT_BIN=$APPDIR/apache-tomcat-6.0.32/bin

# for svn for windows commands and environment variables.
export SVN_BIN=$APPDIR/Subversion-1.6.16/bin
export SVN_EDITOR="vim"

# for ctags commands.
export CTAGS_BIN=$APPDIR/ctags58j2bin

# set commands path.
export PATH=$GVIM_BIN:$JAVA_BIN:$ANT_BIN:$H2_BIN:$TOMCAT_BIN:$SVN_BIN:$CTAGS_BIN:$PATH
---

############################################################
# set ignore file to repository.

Before you execute below operations, maybe you have to execute "svn update" command at
directory that stored this file.

1. Move to sub project directory (i.e. commons)
2. Create .svnignore file.
3. Added ignore files or directoies that you want to the .svnignore file.
4. Execute command "svn propset svn:ignore -F .svnignore ." at sub project directory.
5. Execute command "svn commit <sub project directory name>" at parent directory of
   sub project directory.

