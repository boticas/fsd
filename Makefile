all:
	mvn compile 
	make clean

clean:
	rm -f *.log *.data
