using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace Moment.Api.Migrations
{
    /// <inheritdoc />
    public partial class AddAnniversaryDate : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<DateTime>(
                name: "AnniversaryDate",
                table: "Relationships",
                type: "timestamp with time zone",
                nullable: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "AnniversaryDate",
                table: "Relationships");
        }
    }
}
