package com.igot.cb.transactional.cassandrautils;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.datastax.oss.driver.api.querybuilder.select.Select;
import com.datastax.oss.driver.api.querybuilder.term.Term;
import com.igot.cb.pores.util.Constants;
import com.igot.cb.pores.util.ApiResponse;

import java.text.MessageFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * @author Ruksana
 */
@Component
public class CassandraOperationImpl implements CassandraOperation {

    private Logger logger = LoggerFactory.getLogger(CassandraOperationImpl.class);

    @Autowired
    CassandraConnectionManager connectionManager;

    private Select processQuery(String keyspaceName, String tableName, Map<String, Object> propertyMap,
                                List<String> fields) {
        Select select;
        if (CollectionUtils.isNotEmpty(fields)) {
            select = QueryBuilder.selectFrom(keyspaceName, tableName).columns(fields);
        } else {
            select = QueryBuilder.selectFrom(keyspaceName, tableName).all();
        }
        if (MapUtils.isEmpty(propertyMap)) {
            return select; // Build and return the query
        }
        for (Map.Entry<String, Object> entry : propertyMap.entrySet()) {
            String columnName = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof List) {
                List<?> valueList = (List<?>) value;
                if (CollectionUtils.isNotEmpty(valueList)) {
                    List<Term> terms = valueList.stream()
                            .map(QueryBuilder::literal)
                            .collect(Collectors.toList());
                    select = select.whereColumn(columnName).in(terms);
                }
            } else {
                select = select.whereColumn(columnName).isEqualTo(QueryBuilder.literal(value));
            }
        }
        return select;
    }


    private Select processQueryWithoutFiltering(String keyspaceName, String tableName, Map<String, Object> propertyMap,
                                                List<String> fields) {
        Select selectQuery;
        if (CollectionUtils.isNotEmpty(fields)) {
            selectQuery = QueryBuilder.selectFrom(keyspaceName, tableName).columns(fields);
        } else {
            selectQuery = QueryBuilder.selectFrom(keyspaceName, tableName).all();
        }
        if (MapUtils.isNotEmpty(propertyMap)) {
            for (Map.Entry<String, Object> entry : propertyMap.entrySet()) {
                String columnName = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof List) {
                    List<?> valueList = (List<?>) value;
                    if (CollectionUtils.isNotEmpty(valueList)) {
                        List<Term> terms = valueList.stream()
                                .map(QueryBuilder::literal)
                                .collect(Collectors.toList());
                        selectQuery = selectQuery.whereColumn(columnName).in(terms);
                    }
                } else {
                    selectQuery = selectQuery.whereColumn(columnName).isEqualTo(QueryBuilder.literal(value));
                }
            }
        }
        return selectQuery;
    }

    @Override
    public List<Map<String, Object>> getRecordsByPropertiesByKey(String keyspaceName,
                                                                 String tableName, Map<String, Object> propertyMap, List<String> fields, String key) {
        Select selectQuery = null;
        List<Map<String, Object>> response = new ArrayList<>();
        try {
            selectQuery = processQuery(keyspaceName, tableName, propertyMap, fields);
            CqlSession session = connectionManager.getSession(keyspaceName);
            ResultSet results = session.execute(selectQuery.build());
            response = CassandraUtil.createResponse(results);
            logger.info(response.toString());

        } catch (Exception e) {
            logger.error(Constants.EXCEPTION_MSG_FETCH + tableName + " : " + e.getMessage(), e);
        }
        return response;
    }

    @Override
    public Object insertRecord(String keyspaceName, String tableName, Map<String, Object> request) {
        ApiResponse response = new ApiResponse();
        try {
            String query = CassandraUtil.getPreparedStatement(keyspaceName, tableName, request);
            CqlSession session = connectionManager.getSession(keyspaceName);
            com.datastax.oss.driver.api.core.cql.PreparedStatement statement = session.prepare(query);
            com.datastax.oss.driver.api.core.cql.BoundStatement boundStatement = statement.bind(request.values().toArray());
            session.execute(boundStatement);
            response.put(Constants.RESPONSE, Constants.SUCCESS);
        } catch (Exception e) {
            String errMsg = String.format("Exception occurred while inserting record to %s %s", tableName, e.getMessage());
            logger.error("Error inserting record into {}: {}", tableName, e.getMessage());
            response.put(Constants.RESPONSE, Constants.FAILED);
            response.put(Constants.ERROR_MESSAGE, errMsg);
        }
        return response;
    }

    @Override
    public List<Map<String, Object>> getRecordsByPropertiesWithoutFiltering(String keyspaceName, String tableName, Map<String, Object> propertyMap, List<String> fields, Integer limit) {

        List<Map<String, Object>> response = new ArrayList<>();
        try {
            Select selectQuery = null;
            selectQuery = processQuery(keyspaceName, tableName, propertyMap, fields);

            if (limit != null) selectQuery = selectQuery.limit(limit);
            String queryString = selectQuery.toString();
            SimpleStatement statement = SimpleStatement.newInstance(queryString);
            ResultSet results = connectionManager.getSession(keyspaceName).execute(statement);
            response = CassandraUtil.createResponse(results);

        } catch (Exception e) {
            logger.error("Error fetching records from {}: {}", tableName, e.getMessage());
        }
        return response;
    }

    @Override
    public Map<String,Object> updateRecord(
            String keyspaceName, String tableName, Map<String, Object> request) {
        long startTime = System.currentTimeMillis();
        logger.debug("Cassandra Service updateRecord method started at ==" + startTime);
        Map<String,Object> response = new HashMap<>();
        String query = getUpdateQueryStatement(keyspaceName, tableName, request);
        try {
            PreparedStatement statement = connectionManager.getSession(keyspaceName).prepare(query);
            Object[] array = new Object[request.size()];
            int i = 0;
            String str = "";
            int index = query.lastIndexOf(Constants.SET.trim());
            str = query.substring(index + 4);
            str = str.replace(Constants.EQUAL_WITH_QUE_MARK, "");
            str = str.replace(Constants.WHERE_ID, "");
            str = str.replace(Constants.SEMICOLON, "");
            String[] arr = str.split(",");
            for (String key : arr) {
                array[i++] = request.get(key.trim());
            }
            array[i] = request.get(Constants.ID);
            BoundStatement boundStatement = statement.bind(array);
            connectionManager.getSession(keyspaceName).execute(boundStatement);
            response.put(Constants.RESPONSE, Constants.SUCCESS);
            if (tableName.equalsIgnoreCase(Constants.USER)) {
                logger.info("Cassandra Service updateRecord in user table :" + request);
            }
        } catch (Exception e) {
            if (e.getMessage().contains(Constants.UNKNOWN_IDENTIFIER)) {
                logger.error(
                        Constants.EXCEPTION_MSG_UPDATE + tableName + " : " + e.getMessage(), e);
                String errMsg = String.format("Exception occurred while updating record to to %s %s", tableName, e.getMessage());
                response.put(Constants.RESPONSE, Constants.FAILED);
                response.put(Constants.ERROR_MESSAGE, errMsg);
            }
            logger.error(Constants.EXCEPTION_MSG_UPDATE + tableName + " : " + e.getMessage(), e);
        } finally {
            logQueryElapseTime("updateRecord", startTime, query);
        }
        return response;
    }

    public static String getUpdateQueryStatement(
            String keyspaceName, String tableName, Map<String, Object> map) {
        StringBuilder query =
                new StringBuilder(
                        Constants.UPDATE + keyspaceName + Constants.DOT + tableName + Constants.SET);
        Set<String> key = new HashSet<>(map.keySet());
        key.remove(Constants.ID);
        query.append(String.join(" = ? ,", key));
        query.append(
                Constants.EQUAL_WITH_QUE_MARK + Constants.WHERE_ID + Constants.EQUAL_WITH_QUE_MARK);
        return query.toString();
    }

    protected void logQueryElapseTime(
            String operation, long startTime, String query) {
        logger.info("Cassandra query : " + query);
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        String message =
                "Cassandra operation {0} started at {1} and completed at {2}. Total time elapsed is {3}.";
        MessageFormat mf = new MessageFormat(message);
        logger.debug(mf.format(new Object[] {operation, startTime, stopTime, elapsedTime}));
    }
}
