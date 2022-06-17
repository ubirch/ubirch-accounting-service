package com.ubirch.models.cassandra

import javax.inject.{ Inject, Singleton }

@Singleton
class AcctStoreDAO @Inject() (val events: AcctEventDAO, val owner: AcctEventOwnerDAO)
