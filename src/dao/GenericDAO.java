package dao;

import exception.DatabaseException;

import java.util.List;
import java.util.Optional;

/**
 * Generic Data Access Object (DAO) interface defining standard CRUD operations.
 * Demonstrates Java Generics and decoupling persistence from business logic.
 *
 * @param <T>  The domain entity type
 * @param <ID> The primary key identifier type
 */
public interface GenericDAO<T, ID> {

    /**
     * Persists a new entity into the database.
     *
     * @param entity the entity to persist
     * @return the persisted entity with any generated keys populated
     * @throws DatabaseException if a database access error occurs
     */
    T save(T entity) throws DatabaseException;

    /**
     * Retrieves an entity by its primary key identifier.
     *
     * @param id the entity identifier
     * @return an Optional containing the found entity, or empty if not found
     * @throws DatabaseException if a database access error occurs
     */
    Optional<T> findById(ID id) throws DatabaseException;

    /**
     * Retrieves all entities of this type from the database.
     *
     * @return list of entities
     * @throws DatabaseException if a database access error occurs
     */
    List<T> findAll() throws DatabaseException;

    /**
     * Updates an existing entity.
     *
     * @param entity the entity with updated values
     * @return true if updated successfully, false otherwise
     * @throws DatabaseException if a database access error occurs
     */
    boolean update(T entity) throws DatabaseException;

    /**
     * Deletes an entity by its primary key identifier.
     *
     * @param id the entity identifier
     * @return true if deleted, false otherwise
     * @throws DatabaseException if a database access error occurs
     */
    boolean delete(ID id) throws DatabaseException;
}
