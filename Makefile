.PHONY: format check test

format:
	mvn spotless:apply

check:
	mvn verify

test:
	mvn test
