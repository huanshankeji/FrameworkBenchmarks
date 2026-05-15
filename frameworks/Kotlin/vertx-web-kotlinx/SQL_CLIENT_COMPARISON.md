# `SqlClient` comparison

These results were run on Linux with an AMD Ryzen 9 9950X CPU.

The figures below use the last `db/raw.txt` result (32 threads and 512 connections) for each valid run, in the order requested.

| Variant | Final `Requests/sec` |
| --- | ---: |
| `PgConnection` | `578825.68` |
| `PgBuilder.client()` | `339649.29` |
| `PgBuilder.client().with(PoolOptions().setMaxSize(1))` | `338675.51` |
| `PgBuilder.client().with(PoolOptions().setMaxSize(2))` | `543949.73` |
| `PgBuilder.client().with(PoolOptions().setMaxSize(4))` | `569224.68` |
| `PgBuilder.pool().with(PoolOptions().setMaxSize(4))` | `339562.52` |

According to the results, he default pool size for `PgBuilder.client()` seems to be `1` instead of `4` as in `PoolOptions.DEFAULT_MAX_SIZE`.
