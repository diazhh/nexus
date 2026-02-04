#
# Copyright © 2016-2026 The Thingsboard Authors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

"""
Database Connection Module

Provides SQLAlchemy engine and session management for PostgreSQL.
"""
import logging
from typing import Generator, Optional
from contextlib import contextmanager

from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker, Session
from sqlalchemy.exc import OperationalError

from config import settings

logger = logging.getLogger(__name__)

# Create engine
engine = create_engine(
    settings.database_url,
    pool_size=5,
    max_overflow=10,
    pool_pre_ping=True,
    pool_recycle=3600,
    echo=settings.debug
)

# Session factory
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


def get_db() -> Generator[Session, None, None]:
    """
    Dependency that provides a database session.
    Use with FastAPI Depends().
    """
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


@contextmanager
def get_db_context() -> Generator[Session, None, None]:
    """
    Context manager for database session.
    Use with 'with' statement.
    """
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def check_database_connection() -> dict:
    """
    Check database connectivity.
    Returns status dict with connection details.
    """
    try:
        with engine.connect() as conn:
            result = conn.execute(text("SELECT 1"))
            result.fetchone()

            # Get database version
            version_result = conn.execute(text("SELECT version()"))
            version = version_result.fetchone()[0]

            return {
                "status": "healthy",
                "host": settings.db_host,
                "database": settings.db_name,
                "version": version.split(',')[0] if version else "unknown"
            }
    except OperationalError as e:
        logger.error(f"Database connection failed: {e}")
        return {
            "status": "unhealthy",
            "error": str(e),
            "host": settings.db_host,
            "database": settings.db_name
        }
    except Exception as e:
        logger.error(f"Database check error: {e}")
        return {
            "status": "error",
            "error": str(e)
        }


def verify_ml_tables() -> dict:
    """
    Verify that ML tables exist in the database.
    """
    required_tables = [
        "po_ml_config",
        "po_ml_model",
        "po_ml_prediction",
        "po_ml_training_job",
        "po_ml_feature_stats"
    ]

    try:
        with engine.connect() as conn:
            existing_tables = []
            missing_tables = []

            for table in required_tables:
                result = conn.execute(text(
                    f"SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = '{table}')"
                ))
                exists = result.fetchone()[0]

                if exists:
                    existing_tables.append(table)
                else:
                    missing_tables.append(table)

            return {
                "status": "healthy" if not missing_tables else "degraded",
                "existing_tables": existing_tables,
                "missing_tables": missing_tables
            }
    except Exception as e:
        logger.error(f"Table verification failed: {e}")
        return {
            "status": "error",
            "error": str(e)
        }
