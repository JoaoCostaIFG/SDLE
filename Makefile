compile:
	mvn compile

proxy: compile
	mvn exec:java -Dexec.mainClass=Proj1 -Dexec.args="id1 proxy"

pub: compile
	mvn exec:java -Dexec.mainClass=Proj1 -Dexec.args="id2 pub"

sub: compile
	mvn exec:java -Dexec.mainClass=Proj1 -Dexec.args="id3 sub"

.PHONY: compile proxy pub sub
