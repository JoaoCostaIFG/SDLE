compile:
	mvn compile

proxy: compile
	mvn exec:java -Dexec.args="id1 proxy"

pub: compile
	mvn exec:java -Dexec.args="id2 put bazinga 10"

pub2: compile
	mvn exec:java -Dexec.args="id2 put babide 10"

sub: compile
	mvn exec:java -Dexec.args="id3 get bazinga"

sub2: compile
	mvn exec:java -Dexec.args="id4 get babide 5"

.PHONY: compile proxy pub sub
