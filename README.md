# Job Queues
Processes agents, jobs and jobs requests by allocating an agent to a specific job based on:
- agents availability and pending jobs
- agent's primary and secondary skill set
- job urgency criteria

---

## About the solution
JobQueue was developped in Clojure language. It receives a json-formatted input payload (that is, an array of agents, jobs and job requests) via the console (STDIN) and outputs an array with agent-job allocation (STDOUT), based on job requests and the stablished criteria. 

After that it starts a new cicle wating for a new input.

---

## Prerequisite

To run the JobQueue make sure to:
- have the repository cloned locally
- have [Leiningen](https://leiningen.org/) installed

---

## Running locally

To run the program, type in the console:
```lein run```

After that it will prompt a message asking for the input data via console. On finishing, it outputs the processed content.

The tests can be executed with:
```lein test```