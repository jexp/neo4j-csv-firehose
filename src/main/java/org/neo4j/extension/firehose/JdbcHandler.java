package org.neo4j.extension.firehose;

import org.neo4j.extension.firehose.helper.JdbcHelper;
import org.neo4j.extension.firehose.helper.JdbcMetaData;
import org.neo4j.extension.firehose.helper.JdbcMetaHelper;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.sql.*;
import java.util.Properties;

import static org.neo4j.extension.firehose.helper.StreamingHelper.*;

/**
 * unmanaged extension to neo4j exposing jdbc as a csv file. Cypher's LOAD CSV can use that by doing
 * LOAD CSV WITH HEADERS FROM "http://localhost:7474/csv/jdbc?url=jdbc:mysql....&user=myuser&password=mypass"
 */
@Path("/jdbc")
public class JdbcHandler {

    private Properties parseUrlParameters(UriInfo uriInfo) {
        final Properties props = new Properties();
        uriInfo.getQueryParameters().forEach((key, values) -> {
            if (values.size() > 1) {
                throw new IllegalArgumentException("cannot have more than one value for query param " + key);
            }
            props.put(key, values.get(0));
        });
        return props;
    }

    @GET
    @Path("/")
    public Response jdbcAsCsv(
            @Context UriInfo uriInfo,
            @QueryParam("url") String jdbcString
    ) {

        Properties props = parseUrlParameters(uriInfo);
        return streamCsvResponse(
                csvWriter -> JdbcHelper.runSqlAndConsume(jdbcString, props, resultSet -> {
                    try {
                        csvWriter.writeAll(resultSet, true);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
        );
    }

    @GET
    @Path("/meta")
    @Produces("application/json")
    public JdbcMetaData metadata(
            @Context UriInfo uriInfo,
            @QueryParam("url") String jdbcString
    ) {
        Properties props = parseUrlParameters(uriInfo);
        return JdbcMetaHelper.metaInfo(jdbcString,props);
    }
    
}
