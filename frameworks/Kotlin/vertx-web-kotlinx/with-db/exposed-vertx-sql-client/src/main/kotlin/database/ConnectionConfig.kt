package database

import com.huanshankeji.exposedvertxsqlclient.ConnectionConfig

val connectionConfig get() = ConnectionConfig.Socket(
    host = host,
    port = port,
    user = USER,
    password = PASSWORD,
    database = DATABASE
)
