#!/usr/bin/ruby -w

require 'FileUtils'

basedir = `pwd`.chomp

File.open( 'integration-tests.txt' ).each_line do |testdir|

  testdir = testdir.chomp

  FileUtils.cd "#{basedir}/#{testdir}"

  puts "\n\nRUNNING TEST IN #{testdir}...\n\n"
  puts `mvn \`cat goals.txt\``
  puts "\n\n...done\n\n"

  FileUtils.cd basedir

end
