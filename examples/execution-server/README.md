# Execution server

This example exposes `POST /v1/execute` on `127.0.0.1:8080`. Each request
creates a fresh sandbox, runs one command without shell interpolation, returns
its output as JSON, and destroys the sandbox.

Run `network.nodeops.createos.examples.executionserver.ExecutionServer` as
described in the examples index, then send a request:

```sh
curl --fail-with-body http://127.0.0.1:8080/v1/execute \
  --header 'Content-Type: application/json' \
  --data '{"command":"python3","arguments":["-c","print(sum(range(10)))"]}'
```

The server limits request bodies to 1 MiB and permits four concurrent
executions. Set `EXECUTION_SERVER_ADDRESS` to override its listen address.

> **Warning:** This endpoint runs arbitrary commands and deliberately binds to
> localhost. Add authentication, authorization, rate limiting, audit logging,
> and workload policy before exposing a similar service to a network.
