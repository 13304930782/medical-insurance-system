package com.medical.insurance.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AiMapper {
    @Select("SELECT document_id AS documentId,source_type AS sourceType,source_url AS sourceUrl,title,publisher,published_at AS publishedAt,fetched_at AS fetchedAt,document_status AS documentStatus,created_at AS createdAt FROM ext_ai_knowledge_document ORDER BY created_at DESC")
    List<Map<String,Object>> documents();

    @Select("SELECT document_id FROM ext_ai_knowledge_document WHERE content_hash=#{hash} LIMIT 1")
    Long documentByHash(@Param("hash")String hash);

    @Select("SELECT document_id FROM ext_ai_knowledge_document WHERE source_url=#{url} AND document_status='ACTIVE' LIMIT 1")
    Long documentByUrl(@Param("url")String url);

    @Select("SELECT COUNT(*) AS documentCount,MAX(fetched_at) AS lastFetchedAt FROM ext_ai_knowledge_document WHERE document_status='ACTIVE'")
    Map<String,Object> knowledgeStats();

    @Select("SELECT c.chunk_id AS chunkId,c.document_id AS documentId,c.chunk_content AS chunkContent,d.title,d.source_type AS sourceType,d.source_url AS sourceUrl,d.publisher,MATCH(c.chunk_content) AGAINST(#{query} IN NATURAL LANGUAGE MODE) AS relevance FROM ext_ai_knowledge_chunk c JOIN ext_ai_knowledge_document d ON d.document_id=c.document_id WHERE d.document_status='ACTIVE' AND (MATCH(c.chunk_content) AGAINST(#{query} IN NATURAL LANGUAGE MODE)>0 OR c.chunk_content LIKE CONCAT('%',#{keyword},'%')) ORDER BY relevance DESC,c.chunk_id LIMIT 8")
    List<Map<String,Object>> search(@Param("query")String query,@Param("keyword")String keyword);

    @Select("SELECT c.chunk_id AS chunkId,c.document_id AS documentId,c.chunk_content AS chunkContent,d.title,d.source_type AS sourceType,d.source_url AS sourceUrl,d.publisher,1 AS relevance FROM ext_ai_knowledge_chunk c JOIN ext_ai_knowledge_document d ON d.document_id=c.document_id WHERE d.document_status='ACTIVE' AND d.source_type='SYSTEM_DOCUMENT' ORDER BY d.document_id,c.chunk_index LIMIT 40")
    List<Map<String,Object>> systemEvidence();

    @Select("SELECT document_id FROM ext_ai_knowledge_document WHERE source_type='SYSTEM_DOCUMENT' AND document_status='ACTIVE' ORDER BY document_id LIMIT 1")
    Long defaultHelpDocumentId();

    @Select("SELECT document_id AS documentId,source_type AS sourceType,source_url AS sourceUrl,title,publisher,published_at AS publishedAt,fetched_at AS fetchedAt FROM ext_ai_knowledge_document WHERE document_id=#{id} AND document_status='ACTIVE'")
    Map<String,Object> helpDocument(@Param("id")long id);

    @Select("SELECT chunk_id AS chunkId,chunk_index AS chunkIndex,chunk_content AS chunkContent FROM ext_ai_knowledge_chunk WHERE document_id=#{id} ORDER BY chunk_index")
    List<Map<String,Object>> helpChunks(@Param("id")long id);

    @Delete("DELETE FROM ext_ai_knowledge_document WHERE document_id=#{id} AND source_type!='SYSTEM_DOCUMENT'") int deleteImportedDocument(@Param("id")long id);
}
