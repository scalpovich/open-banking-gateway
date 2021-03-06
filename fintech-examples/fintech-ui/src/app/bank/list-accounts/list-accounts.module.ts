import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { ListAccountsRoutingModule } from './list-accounts-routing.module';
import { ListTransactionsComponent } from '../list-transactions/list-transactions.component';
import { ListAccountsComponent } from './list-accounts.component';
import { RedirectPageComponent } from '../redirect-page/redirect-page.component';
import { RedirectCardComponent } from '../redirect-card/redirect-card.component';

@NgModule({
  declarations: [ListAccountsComponent, ListTransactionsComponent, RedirectPageComponent, RedirectCardComponent],
  imports: [CommonModule, ListAccountsRoutingModule]
})
export class ListAccountsModule {}
