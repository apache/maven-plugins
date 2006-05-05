#!/usr/bin/ruby -w

require 'FileUtils'

class Tester

  attr_reader :failures
  
  def initialize
  
    @failures = Array.new
    
  end

  def test( testdir, basedir )
  
    testdir = testdir.chomp
  
    FileUtils.cd "#{basedir}/#{testdir}"
  
    goals = "clean verify"
    if File.file?( 'goals.txt' )
    
      goals = File.open( 'goals.txt' ).read.chomp
      
    end
    
    puts "\nRUNNING TEST: #{testdir}..."
    
    output = `mvn -X #{goals}`
    
    logfile = File.open( 'log.txt', 'w' ) { |file| file << output }
    
    if output =~ /BUILD SUCCESSFUL/
    
      puts "[SUCCESS]"
    
    else
    
    puts "#{output}\n[ERROR]\n\n"
    @failures << testdir
    
    end
  
    FileUtils.cd basedir
  
  end

end

@basedir = `pwd`.chomp
@tester = Tester.new

if ARGV.size > 0

  ARGV.each { |testdir| @tester.test( testdir, @basedir ) }

else

  File.open( 'integration-tests.txt' ).each_line do |testdir|

    testdir = testdir.chomp
    
    if ( !( testdir =~ /\w*#/ ) && testdir.size > 0 )
    
      @tester.test( testdir, @basedir )
    
    end

  end
  
end

if @tester.failures.size > 0

  puts "\n\nThere were #{@tester.failures.size} failed tests.\nFailures:\n"
  
  @tester.failures.each {|failed| puts "\n- #{failed}"}
  
  puts "\n\n"

end

