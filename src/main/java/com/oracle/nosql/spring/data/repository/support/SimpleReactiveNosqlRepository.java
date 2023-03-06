/*-
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.repository.support;

import java.io.Serializable;

import com.oracle.nosql.spring.data.core.ReactiveNosqlOperations;
import com.oracle.nosql.spring.data.core.query.Criteria;
import com.oracle.nosql.spring.data.core.query.CriteriaQuery;
import com.oracle.nosql.spring.data.core.query.CriteriaType;
import com.oracle.nosql.spring.data.core.query.NosqlQuery;
import com.oracle.nosql.spring.data.repository.NosqlRepository;
import com.oracle.nosql.spring.data.repository.ReactiveNosqlRepository;

import org.reactivestreams.Publisher;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class SimpleReactiveNosqlRepository <T, ID extends Serializable>
    implements ReactiveNosqlRepository<T, ID> {

    private final NosqlEntityInformation<T, ID> entityInformation;
    private final ReactiveNosqlOperations nosqlOperations;

    public SimpleReactiveNosqlRepository(NosqlEntityInformation<T, ID> metadata,
        ApplicationContext applicationContext) {
        this.nosqlOperations =
            applicationContext.getBean(ReactiveNosqlOperations.class);
        this.entityInformation = metadata;

        createTableIfNotExists();
    }

    public SimpleReactiveNosqlRepository(NosqlEntityInformation<T, ID> metadata,
        ReactiveNosqlOperations reactiveNosqlOperations) {
        this.nosqlOperations = reactiveNosqlOperations;
        this.entityInformation = metadata;

        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        nosqlOperations.createTableIfNotExists(entityInformation).block();
    }

    @Override
    public <S extends T> Mono<S> save(S entity) {
        Assert.notNull(entity, "Entity must not be null!");

        if (entityInformation.isNew(entity)) {
            return nosqlOperations.insert(entityInformation, entity);
        } else {
            return nosqlOperations.update(entityInformation, entity);
        }
    }

    @Override
    public <S extends T> Flux<S> saveAll(Iterable<S> entities) {
        Assert.notNull(entities,
            "The given Iterable of entities must not be null!");

        return Flux.fromIterable(entities).flatMap(this::save);
    }

    @Override
    public <S extends T> Flux<S> saveAll(Publisher<S> entityStream) {
        Assert.notNull(entityStream, "The given Publisher of entities must not be null!");

        return Flux.from(entityStream).flatMap(this::save);
    }

    @Override
    public Mono<T> findById(ID id) {
        Assert.notNull(id, "The given id must not be null!");
        return nosqlOperations.findById(entityInformation, id);
    }

    @Override
    public Mono<T> findById(Publisher<ID> idPublisher) {
        Assert.notNull(idPublisher, "The given id must not be null!");

        return Mono.from(idPublisher).flatMap(
            id -> nosqlOperations.findById(entityInformation, id));
    }

    @Override
    public Mono<Boolean> existsById(ID id) {
        Assert.notNull(id, "The given id must not be null!");

        return nosqlOperations.existsById(entityInformation, id);
    }

    @Override
    public Mono<Boolean> existsById(Publisher<ID> publisher) {
        Assert.notNull(publisher, "The given id must not be null!");

        return Mono.from(publisher).flatMap(id ->
            nosqlOperations.existsById(entityInformation, id));
    }

    @Override
    public Flux<T> findAll() {
        return nosqlOperations.findAll(entityInformation);
    }

    @Override
    public Flux<T> findAllById(Iterable<ID> ids) {
        Assert.notNull(ids, "Iterable ids should not be null");

        return findAllById(Flux.fromIterable(ids));
    }

    @Override
    public Flux<T> findAllById(Publisher<ID> idStream) {
        Assert.notNull(idStream,
            "The given Publisher of Id's must not be null!");
        return nosqlOperations.findAllById(entityInformation, idStream);
    }

    @Override
    public Mono<Long> count() {
        return nosqlOperations.count(entityInformation);
    }

    @Override
    public Mono<Void> deleteById(ID id) {
        Assert.notNull(id, "The given id must not be null!");

        return deleteById(Mono.just(id));
    }

    @Override
    public Mono<Void> deleteById(Publisher<ID> idPublisher) {
        Assert.notNull(idPublisher, "Id must not be null!");

        return Mono
                .from(idPublisher)
                .flatMap(id ->
                    nosqlOperations.deleteById( entityInformation, id))
                .then();
    }

    @Override
    public Mono<Void> delete(T entity) {
        Assert.notNull(entity, "entity to be deleted must not be null!");

        final ID id = entityInformation.getId(entity);
        return nosqlOperations.deleteById(entityInformation, id);
    }

    @Override
    public Mono<Void> deleteAllById(Iterable<? extends ID> ids) {
        return Flux.fromIterable(ids).flatMap(this::deleteById).then();
    }

    @Override
    public Mono<Void> deleteAll(Iterable<? extends T> entities) {
        Assert.notNull(entities,
            "The given Iterable of entities must not be null!");

        return Flux.fromIterable(entities).flatMap(this::delete).then();
    }

    @Override
    public Mono<Void> deleteAll(Publisher<? extends T> entityStream) {
        Assert.notNull(entityStream,
            "The given Publisher of entities must not be null!");

        return Flux.from(entityStream)
            .map(entityInformation::getRequiredId)
            .flatMap(this::deleteById)
            .then();
    }

    @Override
    public Mono<Void> deleteAll() {
        return nosqlOperations.deleteAll(entityInformation);
    }

    @Override
    public Flux<T> findAll(Sort sort) {
        Assert.notNull(sort, "Sort must not be null!");

        final NosqlQuery query =
            new CriteriaQuery(Criteria.getInstance(CriteriaType.ALL),
                null).with(sort);

        return nosqlOperations.find(query, entityInformation);
    }

    /**
     * @see ReactiveNosqlRepository#getTimeout()
     */
    @Override
    public int getTimeout() {
        return entityInformation.getTimeout();
    }

    /**
     * @see ReactiveNosqlRepository#setTimeout(int)
     */
    @Override
    public void setTimeout(int milliseconds) {
        entityInformation.setTimeout(milliseconds);
    }

    /**
     * @see ReactiveNosqlRepository#getConsistency()
     */
    @Override
    public String getConsistency() {
        return entityInformation.getConsistency().getType().name();
    }

    /**
     * @see ReactiveNosqlRepository#setConsistency(String)
     */
    @Override
    public void setConsistency(String consistency) {
        entityInformation.setConsistency(consistency);
    }

    /**
     * @see NosqlRepository#getDurability()
     */
    @Override
    public String getDurability() {
        return SimpleNosqlRepository.convertDurability(
            entityInformation.getDurability());
    }

    /**
     * @see NosqlRepository#setDurability(String)
     */
    @Override
    public void setDurability(String durability) {
        entityInformation.setDurability(durability);
    }
}
